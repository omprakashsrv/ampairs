package com.ampairs.core.service

import com.ampairs.core.config.StorageProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.awt.Graphics2D
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO

/**
 * Service for resizing images and generating thumbnails
 */
@Service
class ImageResizingService(
    private val storageProperties: StorageProperties
) {

    private val logger = LoggerFactory.getLogger(ImageResizingService::class.java)

    /**
     * Supported thumbnail sizes
     */
    enum class ThumbnailSize(val pixels: Int, val suffix: String) {
        SMALL(150, "_150"),
        MEDIUM(300, "_300"),
        LARGE(500, "_500");

        companion object {
            fun fromPixels(pixels: Int): ThumbnailSize? {
                return values().find { it.pixels == pixels }
            }

            fun fromSuffix(suffix: String): ThumbnailSize? {
                return values().find { it.suffix == suffix }
            }
        }
    }

    /**
     * Resize image to thumbnail size maintaining aspect ratio
     * @param inputStream Original image input stream
     * @param targetSize Target thumbnail size
     * @param format Output format (jpg, png, webp)
     * @return Resized image as ByteArray
     */
    fun generateThumbnail(
        inputStream: InputStream,
        targetSize: ThumbnailSize,
        format: String = "jpg"
    ): ByteArray {
        return try {
            inputStream.use { input ->
                // Use buffered input stream for better I/O performance
                val bufferedInput = if (input is BufferedInputStream) input else BufferedInputStream(input, 8192)

                val originalImage = ImageIO.read(bufferedInput)
                    ?: throw ImageResizingException("Unable to read image data")

                try {
                    val thumbnailImage = resizeImageMemoryEfficient(originalImage, targetSize.pixels, targetSize.pixels)

                    // Use smaller initial capacity based on thumbnail size
                    val estimatedSize = (targetSize.pixels * targetSize.pixels * 3) / 4 // Rough estimate
                    val outputStream = ByteArrayOutputStream(estimatedSize)

                    // Use JPEG for better compression, PNG for transparency
                    val outputFormat = when (format.lowercase()) {
                        "png" -> "png"
                        "webp" -> "jpg" // WebP not natively supported, fallback to JPG
                        else -> "jpg"
                    }

                    val written = ImageIO.write(thumbnailImage, outputFormat, outputStream)
                    if (!written) {
                        throw ImageResizingException("Failed to write image in format: $outputFormat")
                    }

                    logger.debug("Generated thumbnail: size={}x{}, format={}, originalSize={}x{}",
                        thumbnailImage.width, thumbnailImage.height, outputFormat,
                        originalImage.width, originalImage.height)

                    outputStream.toByteArray()
                } finally {
                    // Explicitly dispose graphics resources to free memory immediately
                    originalImage.flush()
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to generate thumbnail: targetSize={}, format={}, error={}",
                targetSize, format, e.message, e)
            throw ImageResizingException("Failed to generate thumbnail: ${e.message}", e)
        }
    }

    /**
     * Resize image with custom dimensions
     * @param inputStream Original image input stream
     * @param width Target width
     * @param height Target height
     * @param maintainAspectRatio Whether to maintain aspect ratio
     * @param format Output format
     * @return Resized image as ByteArray
     */
    fun resizeImage(
        inputStream: InputStream,
        width: Int,
        height: Int,
        maintainAspectRatio: Boolean = true,
        format: String = "jpg"
    ): ByteArray {
        return try {
            inputStream.use { input ->
                val bufferedInput = if (input is BufferedInputStream) input else BufferedInputStream(input, 8192)

                val originalImage = ImageIO.read(bufferedInput)
                    ?: throw ImageResizingException("Unable to read image data")

                try {
                    val resizedImage = if (maintainAspectRatio) {
                        resizeImageMemoryEfficient(originalImage, width, height)
                    } else {
                        resizeImageExactMemoryEfficient(originalImage, width, height)
                    }

                    val estimatedSize = (width * height * 3) / 4
                    val outputStream = ByteArrayOutputStream(estimatedSize)
                    val outputFormat = when (format.lowercase()) {
                        "png" -> "png"
                        "webp" -> "jpg"
                        else -> "jpg"
                    }

                    ImageIO.write(resizedImage, outputFormat, outputStream)
                    outputStream.toByteArray()
                } finally {
                    originalImage.flush()
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to resize image: width={}, height={}, error={}",
                width, height, e.message, e)
            throw ImageResizingException("Failed to resize image: ${e.message}", e)
        }
    }

    /**
     * Check if image exceeds maximum dimensions and needs resizing
     */
    fun shouldResizeForUpload(width: Int, height: Int): Boolean {
        return width > storageProperties.image.maxWidth || height > storageProperties.image.maxHeight
    }

    /**
     * Resize image for upload if it exceeds maximum dimensions
     */
    fun resizeForUpload(inputStream: InputStream, format: String = "jpg"): ByteArray {
        return resizeImage(
            inputStream,
            storageProperties.image.maxWidth,
            storageProperties.image.maxHeight,
            maintainAspectRatio = true,
            format = format
        )
    }

    /**
     * Get image dimensions without loading the full image
     */
    fun getImageDimensions(inputStream: InputStream): Pair<Int, Int>? {
        return try {
            val readers = ImageIO.getImageReadersByFormatName("jpg")
            if (!readers.hasNext()) return null

            val reader = readers.next()
            reader.input = ImageIO.createImageInputStream(inputStream)
            val width = reader.getWidth(0)
            val height = reader.getHeight(0)
            reader.dispose()

            Pair(width, height)
        } catch (e: Exception) {
            logger.warn("Failed to get image dimensions: {}", e.message)
            null
        }
    }

    /**
     * Generate thumbnail filename
     */
    fun generateThumbnailKey(originalKey: String, size: ThumbnailSize): String {
        val lastDotIndex = originalKey.lastIndexOf('.')
        return if (lastDotIndex > 0) {
            val basePath = originalKey.substring(0, lastDotIndex)
            val extension = originalKey.substring(lastDotIndex)
            "${basePath}${size.suffix}${extension}"
        } else {
            "${originalKey}${size.suffix}"
        }
    }

    /**
     * Generate thumbnail storage path
     */
    fun generateThumbnailPath(originalPath: String, size: ThumbnailSize): String {
        val pathParts = originalPath.split("/")
        val fileName = pathParts.last()
        val directory = pathParts.dropLast(1).joinToString("/")

        val thumbnailFileName = generateThumbnailKey(fileName, size)
        return "$directory/thumbs/$thumbnailFileName"
    }

    /**
     * Check if format supports transparency
     */
    fun supportsTransparency(format: String): Boolean {
        return format.lowercase() in listOf("png", "gif", "webp")
    }

    private fun resizeImage(originalImage: BufferedImage, maxWidth: Int, maxHeight: Int): BufferedImage {
        val originalWidth = originalImage.width
        val originalHeight = originalImage.height

        // Calculate new dimensions maintaining aspect ratio
        val aspectRatio = originalWidth.toDouble() / originalHeight.toDouble()
        val (newWidth, newHeight) = if (originalWidth > originalHeight) {
            val width = minOf(maxWidth, originalWidth)
            val height = (width / aspectRatio).toInt()
            width to height
        } else {
            val height = minOf(maxHeight, originalHeight)
            val width = (height * aspectRatio).toInt()
            width to height
        }

        return resizeImageExactMemoryEfficient(originalImage, newWidth, newHeight)
    }

    /**
     * Memory-efficient resizing maintaining aspect ratio
     */
    private fun resizeImageMemoryEfficient(originalImage: BufferedImage, maxWidth: Int, maxHeight: Int): BufferedImage {
        val originalWidth = originalImage.width
        val originalHeight = originalImage.height

        // Calculate new dimensions maintaining aspect ratio
        val aspectRatio = originalWidth.toDouble() / originalHeight.toDouble()
        val (newWidth, newHeight) = if (originalWidth > originalHeight) {
            val width = minOf(maxWidth, originalWidth)
            val height = (width / aspectRatio).toInt()
            width to height
        } else {
            val height = minOf(maxHeight, originalHeight)
            val width = (height * aspectRatio).toInt()
            width to height
        }

        return resizeImageExactMemoryEfficient(originalImage, newWidth, newHeight)
    }

    /**
     * Memory-efficient exact resizing using progressive scaling for large images
     */
    private fun resizeImageExactMemoryEfficient(originalImage: BufferedImage, width: Int, height: Int): BufferedImage {
        val originalWidth = originalImage.width
        val originalHeight = originalImage.height

        // For very large images (>4x target size), use progressive scaling
        if (originalWidth > width * 4 || originalHeight > height * 4) {
            return progressiveResize(originalImage, width, height)
        }

        return directResize(originalImage, width, height)
    }

    /**
     * Direct resize for moderate size differences
     */
    private fun directResize(originalImage: BufferedImage, width: Int, height: Int): BufferedImage {
        // Use TYPE_INT_RGB for JPEG, preserve alpha for PNG
        val imageType = if (originalImage.colorModel.hasAlpha()) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
        val resizedImage = BufferedImage(width, height, imageType)
        val graphics2D: Graphics2D = resizedImage.createGraphics()

        try {
            // High quality rendering hints
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics2D.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)

            graphics2D.drawImage(originalImage, 0, 0, width, height, null)
        } finally {
            graphics2D.dispose()
        }

        return resizedImage
    }

    /**
     * Progressive resize for very large images to reduce memory usage
     */
    private fun progressiveResize(originalImage: BufferedImage, targetWidth: Int, targetHeight: Int): BufferedImage {
        var currentImage = originalImage
        var currentWidth = originalImage.width
        var currentHeight = originalImage.height

        // Scale down in steps, halving dimensions until we're close to target
        while (currentWidth > targetWidth * 2 || currentHeight > targetHeight * 2) {
            val stepWidth = maxOf(targetWidth, currentWidth / 2)
            val stepHeight = maxOf(targetHeight, currentHeight / 2)

            val intermediateImage = directResize(currentImage, stepWidth, stepHeight)

            // Free the previous image if it's not the original
            if (currentImage != originalImage) {
                currentImage.flush()
            }

            currentImage = intermediateImage
            currentWidth = stepWidth
            currentHeight = stepHeight
        }

        // Final resize to exact target dimensions
        val finalImage = directResize(currentImage, targetWidth, targetHeight)

        // Clean up intermediate image
        if (currentImage != originalImage) {
            currentImage.flush()
        }

        return finalImage
    }

}

/**
 * Exception for image resizing operations
 */
class ImageResizingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)