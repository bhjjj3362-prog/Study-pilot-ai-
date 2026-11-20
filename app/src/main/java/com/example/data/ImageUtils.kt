package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_IMAGE_DIMENSION_PX = 1600

object ImageUtils {

    fun createCameraPhotoUri(context: Context): Uri {
        val photosDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
        val photoFile = File.createTempFile("doubt_", ".jpg", photosDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile,
        )
    }

    suspend fun decodeScaledBitmap(
        context: Context,
        uri: Uri,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return@withContext null

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return@withContext null
        }

        val sampleSize = calculateInSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            maxDimension = MAX_IMAGE_DIMENSION_PX,
        )

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return@withContext null

        val rotationDegrees = readExifRotationDegrees(context, uri)
        val oriented = if (rotationDegrees != 0f) {
            rotateBitmap(decoded, rotationDegrees).also {
                if (it !== decoded) decoded.recycle()
            }
        } else {
            decoded
        }

        downscaleIfNeeded(oriented, MAX_IMAGE_DIMENSION_PX)
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        maxDimension: Int,
    ): Int {
        var inSampleSize = 1
        val maxSide = maxOf(width, height)
        while ((maxSide / inSampleSize) > maxDimension * 2) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun readExifRotationDegrees(context: Context, uri: Uri): Float {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (_: IOException) {
            0f
        }
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun downscaleIfNeeded(source: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = maxOf(source.width, source.height)
        if (maxSide <= maxDimension) return source

        val scale = maxDimension.toFloat() / maxSide.toFloat()
        val targetWidth = (source.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true).also {
            if (it !== source) source.recycle()
        }
    }
}
