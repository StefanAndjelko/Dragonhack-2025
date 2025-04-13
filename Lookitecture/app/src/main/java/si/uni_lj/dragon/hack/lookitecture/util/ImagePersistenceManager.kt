package si.uni_lj.dragon.hack.lookitecture.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Manages persistent storage of images in the app with optimized performance
 */
    object ImagePersistenceManager {
    private const val TAG = "ImagePersistenceManager"
    private const val IMAGES_DIRECTORY = "lookitecture_images"
    private const val MAX_IMAGE_WIDTH = 800 // Reduced from 1200 for faster processing
    private const val MAX_IMAGE_HEIGHT = 1000 // Reduced from 1600 for faster processing
    private const val COMPRESSION_QUALITY = 80 // Slightly reduced for faster saving

    /**
     * Save an image from a Uri to internal storage and return its persistent path
     * Optimized for performance with smaller image size and more aggressive scaling
     */
    suspend fun saveImageFromUri(context: Context, imageUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Check if the URI is already pointing to our internal storage
                if (imageUri.path?.contains(IMAGES_DIRECTORY) == true) {
                    val file = File(imageUri.path!!)
                    if (file.exists()) {
                        return@withContext file.absolutePath
                    }
                }

                // Create images directory if needed (done once at startup)
                val imagesDir = File(context.filesDir, IMAGES_DIRECTORY).apply {
                    if (!exists()) mkdirs()
                }

                // Generate a simpler filename for speed
                val filename = "img_${System.currentTimeMillis()}.jpg"
                val imageFile = File(imagesDir, filename)

                // Use a more direct approach for certain URI types
                if (imageUri.scheme == "file") {
                    try {
                        val inputFile = File(imageUri.path!!)
                        if (inputFile.exists() && inputFile.length() < 5 * 1024 * 1024) {
                            // For small files, direct copy is fastest
                            inputFile.copyTo(imageFile, overwrite = true)
                            return@withContext imageFile.absolutePath
                        }
                    } catch (e: Exception) {
                        // Fall through to bitmap approach
                    }
                }

                // Fast decoding options
                val options = android.graphics.BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565 // Less memory than ARGB_8888
                    inSampleSize = 2 // Immediate downsampling for all images
                }

                // For content URIs, use bitmap approach with aggressive downsampling
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    val bitmap = android.graphics.BitmapFactory.decodeStream(input, null, options)

                    // If bitmap is still too large, scale it down
                    val scaledBitmap = if (bitmap != null &&
                        (bitmap.width > MAX_IMAGE_WIDTH || bitmap.height > MAX_IMAGE_HEIGHT)) {

                        val scaleFactor = calculateScaleFactor(bitmap.width, bitmap.height)
                        val targetWidth = bitmap.width / scaleFactor
                        val targetHeight = bitmap.height / scaleFactor

                        Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true).also {
                            // Recycle the original to free memory
                            bitmap.recycle()
                        }
                    } else {
                        bitmap
                    }

                    if (scaledBitmap != null) {
                        FileOutputStream(imageFile).use { output ->
                            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, output)
                            output.flush()
                        }

                        scaledBitmap.recycle()
                        return@withContext imageFile.absolutePath
                    }
                }

                return@withContext null
            } catch (e: Exception) {
                Log.e(TAG, "Error saving image: ${e.message}")
                return@withContext null
            }
        }
    }

    /**
     * Calculate appropriate scale factor for large images (faster algorithm)
     */
    private fun calculateScaleFactor(width: Int, height: Int): Int {
        val widthRatio = width / MAX_IMAGE_WIDTH
        val heightRatio = height / MAX_IMAGE_HEIGHT

        return max(1, max(widthRatio, heightRatio))
    }

    /**
     * Get a shareable URI for an image file path
     */
    fun getUriFromPath(context: Context, path: String): Uri? {
        return try {
            File(path).toUri()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fast check if an image exists at the given path
     */
    fun imageExists(path: String?): Boolean {
        if (path == null) return false
        val file = File(path)
        return file.exists()
    }
}