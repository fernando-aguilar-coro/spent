package com.app.spent.util

import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri

object ImageCompressor {

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

        // 2. Calculate inSampleSize
        val inSampleSize = calculateInSampleSize(options, maxDimension)

        // 3. Decode scaled bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val decodedBitmap = context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        } ?: throw IllegalStateException("Could not decode image from URI: $sourceUri")

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

    /**
     * Calculates the inSampleSize for sub-sampling large bitmaps in memory.
     */
    fun calculateInSampleSize(options: BitmapFactory.Options, maxDimension: Int): Int {
        val originalWidth = options.outWidth
        val originalHeight = options.outHeight
        var inSampleSize = 1
        val maxSide = max(originalWidth, originalHeight)
        if (maxSide > maxDimension) {
            inSampleSize = maxSide / maxDimension
        }
        return max(1, inSampleSize)
    }

    /**
     * Detects EXIF rotation tag and rotates bitmap to standard upright orientation.
     */
    fun fixOrientation(context: Context, sourceUri: Uri, bitmap: Bitmap): Bitmap {
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
