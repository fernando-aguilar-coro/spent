package com.app.spent.util

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.math.max

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.app.spent.R
import com.app.spent.data.sync.GoogleDriveRestService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageStorageHelper {

    const val DESTINATION_DEVICE = "DEVICE"
    const val DESTINATION_IN_APP = "IN_APP"
    const val DESTINATION_GOOGLE_DRIVE = "GOOGLE_DRIVE"

    /**
     * Processes and stores an image based on the chosen destination setting.
     */
    suspend fun processAndSaveImage(
        context: Context,
        sourceUri: Uri,
        destinationType: String
    ): Result<String> = withContext(Dispatchers.IO) {
        when (destinationType) {
            DESTINATION_IN_APP -> saveToInAppStorage(context, sourceUri)
            DESTINATION_GOOGLE_DRIVE -> saveToGoogleDrive(context, sourceUri)
            else -> saveToDeviceStorage(context, sourceUri)
        }
    }

    /**
     * Saves image to Device public/shared storage (Pictures/Spent).
     */
    suspend fun saveToDeviceStorage(context: Context, sourceUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val fileName = "spent_receipt_${UUID.randomUUID()}.jpg"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + File.separator + "Spent"
                        )
                    }

                    val uri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    ) ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    Result.success(uri.toString())
                } else {
                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val spentDir = File(picturesDir, "Spent").apply { mkdirs() }
                    val targetFile = File(spentDir, fileName)

                    FileOutputStream(targetFile).use { outputStream ->
                        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    Result.success(Uri.fromFile(targetFile).toString())
                }
            } catch (e: Exception) {
                // Fallback to app external files dir if MediaStore fails
                try {
                    val extDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                        ?: context.filesDir
                    val targetFile = File(extDir, "spent_receipt_${UUID.randomUUID()}.jpg")
                    FileOutputStream(targetFile).use { outputStream ->
                        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Result.success(Uri.fromFile(targetFile).toString())
                } catch (fallbackEx: Exception) {
                    fallbackEx.printStackTrace()
                    Result.failure(e)
                }
            }
        }

    /**
     * Saves image securely in the app's internal private storage directory.
     */
    suspend fun saveToInAppStorage(context: Context, sourceUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val receiptsDir = File(context.filesDir, "receipt_images").apply { mkdirs() }
                val targetFile = File(receiptsDir, "spent_receipt_${UUID.randomUUID()}.jpg")

                FileOutputStream(targetFile).use { outputStream ->
                    context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                Result.success(Uri.fromFile(targetFile).toString())
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    /**
     * Compresses the image and uploads it to Google Drive.
     */
    suspend fun saveToGoogleDrive(context: Context, sourceUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val account = GoogleDriveRestService.getSignedInAccount(context)
                    ?: return@withContext Result.failure(
                        Exception(context.getString(R.string.drive_not_connected_warning))
                    )

                // Compress image before uploading to Drive
                val compressedBytes = compressImageBitmap(context, sourceUri, maxDimension = 1280, quality = 75)
                val fileName = "spent_receipt_${UUID.randomUUID()}.jpg"

                GoogleDriveRestService.uploadReceiptImage(context, account, compressedBytes, fileName)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    /**
     * Downscales and compresses a bitmap from Uri into a memory-efficient JPEG byte array.
     */
    fun compressImageBitmap(
        context: Context,
        sourceUri: Uri,
        maxDimension: Int = 1280,
        quality: Int = 75
    ): ByteArray {
        // 1. Decode bounds
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }

        val originalWidth = options.outWidth
        val originalHeight = options.outHeight

        // 2. Calculate inSampleSize
        var inSampleSize = 1
        val maxSide = max(originalWidth, originalHeight)
        if (maxSide > maxDimension) {
            inSampleSize = maxSide / maxDimension
        }

        // 3. Decode scaled bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = max(1, inSampleSize)
        }
        val decodedBitmap = context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        } ?: throw IllegalStateException("Could not decode image from URI")

        // 4. Handle EXIF rotation
        val rotatedBitmap = fixOrientation(context, sourceUri, decodedBitmap)

        // 5. Final resize if still slightly above maxDimension
        val currentMax = max(rotatedBitmap.width, rotatedBitmap.height)
        val finalBitmap = if (currentMax > maxDimension) {
            val scale = maxDimension.toFloat() / currentMax.toFloat()
            val newW = (rotatedBitmap.width * scale).toInt()
            val newH = (rotatedBitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(rotatedBitmap, newW, newH, true)
        } else {
            rotatedBitmap
        }

        // 6. Compress to JPEG bytes
        val byteStream = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteStream)
        return byteStream.toByteArray()
    }

    private fun fixOrientation(context: Context, sourceUri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            val exif = inputStream?.use { ExifInterface(it) } ?: return bitmap
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            bitmap
        }
    }
}
