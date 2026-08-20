package com.app.spent.util

import java.io.File

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore

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

        // 2. Direct parsing check (file:// or content://)
        try {
            val parsedUri = Uri.parse(trimmed)
            if (parsedUri.scheme == "file") {
                val file = parsedUri.path?.let { File(it) }
                if (file != null && file.exists()) {
                    return parsedUri
                }
            } else if (parsedUri.scheme == "content") {
                // Verify content resolver can open the stream
                try {
                    context.contentResolver.openInputStream(parsedUri)?.use {
                        return parsedUri
                    }
                } catch (_: Exception) {
                    // Content ID changed (e.g. after reinstall) - fallback to filename lookup below
                }
            }
        } catch (_: Exception) {}

        // 3. Extract filename / ID and check known storage directories
        val fileName = extractReceiptFileName(trimmed)
        if (fileName.isNotBlank()) {
            // Check in-app private storage
            val inAppFile = File(File(context.filesDir, "receipt_images"), fileName)
            if (inAppFile.exists()) {
                return Uri.fromFile(inAppFile)
            }

            // Check external files dir
            val extDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (extDir != null) {
                val extFile = File(extDir, fileName)
                if (extFile.exists()) {
                    return Uri.fromFile(extFile)
                }
            }

            // Check public Pictures/Spent
            val publicDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Spent"
            )
            val publicFile = File(publicDir, fileName)
            if (publicFile.exists()) {
                return Uri.fromFile(publicFile)
            }

            // Query MediaStore by DISPLAY_NAME
            queryMediaStoreForImage(context, fileName)?.let {
                return it
            }
        }

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
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
