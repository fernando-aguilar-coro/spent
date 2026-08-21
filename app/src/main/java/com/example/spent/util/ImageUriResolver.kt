package com.app.spent.util

import java.io.File

import android.content.Context
import android.net.Uri
import android.os.Environment

object ImageUriResolver {

    /**
     * Resolves an image URI, web URL, or filename into a displayable model for Coil.
     */
    fun resolve(context: Context, rawUriOrFileName: String?): Any? {
        if (rawUriOrFileName.isNullOrBlank()) return null
        val trimmed = rawUriOrFileName.trim()

        // 1. Google Drive or Web URL
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }

        // 2. Direct URI parsing (file:// or content://)
        try {
            val uri = Uri.parse(trimmed)
            if (uri.scheme == "file") {
                val file = uri.path?.let { File(it) }
                if (file != null && file.exists()) {
                    return file
                }
            } else if (uri.scheme == "content") {
                return uri
            }
        } catch (_: Exception) {}

        // 3. Filename check in storage directories
        val fileName = extractReceiptFileName(trimmed)
        if (fileName.isNotBlank()) {
            // Check in-app private storage
            val inAppFile = File(File(context.filesDir, "receipt_images"), fileName)
            if (inAppFile.exists()) {
                return inAppFile
            }

            // Check external app storage
            val extDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (extDir != null) {
                val extFile = File(extDir, fileName)
                if (extFile.exists()) {
                    return extFile
                }
            }

            // Check public Pictures/Spent
            val publicDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Spent"
            )
            val publicFile = File(publicDir, fileName)
            if (publicFile.exists()) {
                return publicFile
            }
        }

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
}
