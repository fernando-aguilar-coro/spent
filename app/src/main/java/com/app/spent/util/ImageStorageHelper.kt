package com.app.spent.util

import java.io.File
import java.io.FileOutputStream
import java.util.UUID

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.app.spent.R
import com.app.spent.data.sync.GoogleDriveRestService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageStorageHelper {

    const val DESTINATION_GOOGLE_DRIVE = "GOOGLE_DRIVE"
    const val DESTINATION_IN_APP = "IN_APP"
    const val DESTINATION_DEVICE = "DEVICE"

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
            DESTINATION_DEVICE -> saveToDeviceStorage(context, sourceUri)
            else -> saveToGoogleDrive(context, sourceUri)
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
     * Saves image securely in the app's internal private storage directory.
     */
    suspend fun saveToInAppStorage(context: Context, sourceUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val compressedBytes = ImageCompressor.compressImageBitmap(
                    context = context,
                    sourceUri = sourceUri,
                    maxDimension = 1280,
                    quality = 85
                )
                val receiptsDir = File(context.filesDir, "receipt_images").apply { mkdirs() }
                val targetFile = File(receiptsDir, "spent_receipt_${UUID.randomUUID()}.jpg")

                FileOutputStream(targetFile).use { it.write(compressedBytes) }

                Result.success(Uri.fromFile(targetFile).toString())
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    /**
     * Saves image to Device external files directory.
     */
    suspend fun saveToDeviceStorage(context: Context, sourceUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val compressedBytes = ImageCompressor.compressImageBitmap(
                    context = context,
                    sourceUri = sourceUri,
                    maxDimension = 1280,
                    quality = 85
                )
                val extDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?: context.filesDir
                val targetFile = File(extDir, "spent_receipt_${UUID.randomUUID()}.jpg")

                FileOutputStream(targetFile).use { it.write(compressedBytes) }

                Result.success(Uri.fromFile(targetFile).toString())
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

            // 3. Delete files in public Pictures/Spent if any exist
            val publicDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Spent"
            )
            if (publicDir.exists()) {
                publicDir.listFiles()?.filter { it.name.startsWith("spent_receipt_") }?.forEach { it.delete() }
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
