package com.app.spent.util

import java.io.File

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider

object ImageUriResolver {

    /**
     * Resolves an image URI or filename to an accessible Uri/File/URL for Coil image loading.
     * Reconnects to existing device storage files by filename/ID if the app was reinstalled.
     */
    fun resolve(context: Context, rawUriOrFileName: String?): Any? {
        if (rawUriOrFileName.isNullOrBlank()) return null

        val trimmed = rawUriOrFileName.trim()

        // 1. Google Drive or Web URL
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }

        val fileName = extractReceiptFileName(trimmed)

        // 2. Try MediaStore query first (the official Android way to load public shared images across reinstalls)
        if (fileName.isNotBlank()) {
            queryMediaStoreForImage(context, fileName)?.let { mediaStoreUri ->
                return mediaStoreUri
            }

            // 3. Check in-app private storage (filesDir/receipt_images)
            val inAppFile = File(File(context.filesDir, "receipt_images"), fileName)
            if (inAppFile.exists()) {
                return inAppFile
            }

            // 4. Check external files dir (getExternalFilesDir)
            val extDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (extDir != null) {
                val extFile = File(extDir, fileName)
                if (extFile.exists()) {
                    return extFile
                }
            }

            // 5. Check public Pictures/Spent
            val publicDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Spent"
            )
            val publicFile = File(publicDir, fileName)
            if (publicFile.exists()) {
                // Try FileProvider first to get a shareable content:// URI
                try {
                    val fileProviderUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        publicFile
                    )
                    context.contentResolver.openInputStream(fileProviderUri)?.use {
                        return fileProviderUri
                    }
                } catch (_: Exception) {}

                return publicFile
            }
        }

        // 6. Direct parsing check (file:// or content://)
        try {
            val parsedUri = Uri.parse(trimmed)
            if (parsedUri.scheme == "file") {
                val file = parsedUri.path?.let { File(it) }
                if (file != null && file.exists()) {
                    return file
                }
            } else if (parsedUri.scheme == "content") {
                try {
                    context.contentResolver.openInputStream(parsedUri)?.use {
                        return parsedUri
                    }
                } catch (_: Exception) {
                    // Content ID changed (e.g. after reinstall)
                }
            }
        } catch (_: Exception) {}

        // Fallback to original string so Coil can attempt default decoding
        return rawUriOrFileName
    }

    /**
     * Extracts the spent receipt filename (e.g. spent_receipt_UUID.jpg) from a full URI or path.
     */
    fun extractReceiptFileName(rawUri: String): String {
        val lastSegment = rawUri.substringAfterLast("/").substringAfterLast("\\").substringBefore("?")
        return if (lastSegment.contains("spent_receipt_")) {
            val idx = lastSegment.indexOf("spent_receipt_")
            lastSegment.substring(idx)
        } else {
            lastSegment
        }
    }

    /**
     * Queries MediaStore for an image with the matching DISPLAY_NAME.
     */
    fun queryMediaStoreForImage(context: Context, fileName: String): Uri? {
        return try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME
            )
            val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf(fileName, "%$fileName%")
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Images.Media._ID} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    try {
                        context.contentResolver.openInputStream(uri)?.close()
                        return uri
                    } catch (_: Exception) {
                        return uri
                    }
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
