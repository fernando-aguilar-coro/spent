package com.app.spent.util

import java.io.File
import java.io.FileOutputStream
import java.util.UUID

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
     * Compresses the image and returns the standardized relative path (Pictures/Spent/spent_receipt_UUID.jpg)
     * so it can be reconnected across app reinstalls and JSON backup restores.
     */
    suspend fun saveToDeviceStorage(context: Context, sourceUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val fileName = "spent_receipt_${UUID.randomUUID()}.jpg"
                val standardizedPath = "Pictures/Spent/$fileName"

                // Compress image to optimize storage and correct EXIF orientation
                val compressedBytes = ImageCompressor.compressImageBitmap(
                    context = context,
                    sourceUri = sourceUri,
                    maxDimension = 1280,
                    quality = 85
                )

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
                        outputStream.write(compressedBytes)
                    }

                    // Also save a fallback copy in external pictures dir
                    try {
                        val extDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                        if (extDir != null) {
                            val extFile = File(extDir, fileName)
                            FileOutputStream(extFile).use { it.write(compressedBytes) }
                        }
                    } catch (_: Exception) {}

                    Result.success(standardizedPath)
                } else {
                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val spentDir = File(picturesDir, "Spent").apply { mkdirs() }
                    val targetFile = File(spentDir, fileName)

                    FileOutputStream(targetFile).use { outputStream ->
                        outputStream.write(compressedBytes)
                    }

                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath),
                        arrayOf("image/jpeg"),
                        null
                    )

                    Result.success(standardizedPath)
                }
            } catch (e: Exception) {
                // Fallback to app external files dir if MediaStore fails
                try {
                    val extDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                        ?: context.filesDir
                    val fileName = "spent_receipt_${UUID.randomUUID()}.jpg"
                    val targetFile = File(extDir, fileName)
                    FileOutputStream(targetFile).use { outputStream ->
                        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Result.success("Pictures/Spent/$fileName")
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
     * Compresses the image using ImageCompressor and uploads it to Google Drive.
     */
    suspend fun saveToGoogleDrive(context: Context, sourceUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val account = GoogleDriveRestService.getSignedInAccount(context)
                    ?: return@withContext Result.failure(
                        Exception(context.getString(R.string.drive_not_connected_warning))
                    )

                // Compress image before uploading to Drive using extracted ImageCompressor
                val compressedBytes = ImageCompressor.compressImageBitmap(
                    context = context,
                    sourceUri = sourceUri,
                    maxDimension = 1280,
                    quality = 75
                )
                val fileName = "spent_receipt_${UUID.randomUUID()}.jpg"

                GoogleDriveRestService.uploadReceiptImage(context, account, compressedBytes, fileName)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    /**
     * Resolves an image URI or filename to an accessible Uri/File/URL.
     * Delegates to [ImageUriResolver.resolve].
     */
    fun resolveImageUri(context: Context, rawUriOrFileName: String?): Any? {
        return ImageUriResolver.resolve(context, rawUriOrFileName)
    }

    /**
     * Extracts the spent receipt filename.
     * Delegates to [ImageUriResolver.extractReceiptFileName].
     */
    fun extractReceiptFileName(rawUri: String): String {
        return ImageUriResolver.extractReceiptFileName(rawUri)
    }

    /**
     * Deletes all local and device stored images created by Spent (for Settings data reset).
     */
    fun deleteAllStoredImages(context: Context) {
        try {
            // 1. Delete in-app private receipt images
            val inAppDir = File(context.filesDir, "receipt_images")
            if (inAppDir.exists()) {
                inAppDir.deleteRecursively()
            }

            // 2. Delete external app files receipt images
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let { extDir ->
                if (extDir.exists()) {
                    extDir.listFiles()?.filter { it.name.startsWith("spent_receipt_") }?.forEach { it.delete() }
                }
            }

            // 3. Delete files in public Pictures/Spent
            val publicDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Spent"
            )
            if (publicDir.exists()) {
                publicDir.listFiles()?.filter { it.name.startsWith("spent_receipt_") }?.forEach { it.delete() }
            }

            // 4. Delete MediaStore entries created by Spent
            try {
                val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("spent_receipt_%")
                context.contentResolver.delete(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    selection,
                    selectionArgs
                )
            } catch (me: Exception) {
                me.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Deletes a single image given its URI or filename.
     */
    fun deleteImage(context: Context, rawUriOrFileName: String?) {
        if (rawUriOrFileName.isNullOrBlank()) return
        try {
            val fileName = extractReceiptFileName(rawUriOrFileName)
            if (fileName.isNotBlank()) {
                File(File(context.filesDir, "receipt_images"), fileName).delete()
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let {
                    File(it, fileName).delete()
                }
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Spent/$fileName"
                ).delete()

                try {
                    val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
                    val selectionArgs = arrayOf(fileName)
                    context.contentResolver.delete(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        selection,
                        selectionArgs
                    )
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Downscales and compresses a bitmap from Uri into a memory-efficient JPEG byte array.
     * Backwards-compatible delegate to [ImageCompressor.compressImageBitmap].
     */
    fun compressImageBitmap(
        context: Context,
        sourceUri: Uri,
        maxDimension: Int = 1280,
        quality: Int = 75
    ): ByteArray {
        return ImageCompressor.compressImageBitmap(context, sourceUri, maxDimension, quality)
    }
}
