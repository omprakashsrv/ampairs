package com.ampairs.customer.data.repository

import com.ampairs.customer.util.CustomerLogger
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PickerMode

/**
 * FileKit-based implementation of ImageFilePicker.
 * Provides cross-platform image file selection using FileKit.
 */
class FileKitImagePicker : ImageFilePicker {

    companion object {
        private val SUPPORTED_IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10MB
    }

    override suspend fun pickImage(): ImageFilePickerResult? {
        return try {
            CustomerLogger.d("ImageFilePicker", "Starting single image picker")

            val file = FileKit.pickFile(
                type = PickerType.Image,
                mode = PickerMode.Single
            )

            if (file == null) {
                CustomerLogger.d("ImageFilePicker", "User cancelled file selection")
                return null
            }

            val result = processPickedFile(file)
            CustomerLogger.d("ImageFilePicker", "Successfully picked image: ${result?.fileName}")
            result
        } catch (e: Exception) {
            CustomerLogger.e("ImageFilePicker", "Error picking image", e)
            null
        }
    }

    override suspend fun pickMultipleImages(maxCount: Int): List<ImageFilePickerResult> {
        return try {
            CustomerLogger.d("ImageFilePicker", "Starting multiple image picker (max: $maxCount)")

            val files = FileKit.pickFile(
                type = PickerType.Image,
                mode = PickerMode.Multiple()
            )

            if (files == null) {
                CustomerLogger.d("ImageFilePicker", "User cancelled file selection")
                return emptyList()
            }

            // For multiple mode, files should be a List<PlatformFile>
            val fileList = (files as? List<io.github.vinceglb.filekit.core.PlatformFile>) ?: listOf(files as io.github.vinceglb.filekit.core.PlatformFile)

            val results = fileList.take(maxCount).mapNotNull { file ->
                processPickedFile(file)
            }

            CustomerLogger.d("ImageFilePicker", "Successfully picked ${results.size} images")
            results
        } catch (e: Exception) {
            CustomerLogger.e("ImageFilePicker", "Error picking multiple images", e)
            emptyList()
        }
    }

    private suspend fun processPickedFile(file: io.github.vinceglb.filekit.core.PlatformFile): ImageFilePickerResult? {
        return try {
            val fileName = file.name
            val fileSize = file.getSize() ?: 0L

            // Validate file extension
            val extension = fileName.substringAfterLast(".", "").lowercase()
            if (extension !in SUPPORTED_IMAGE_EXTENSIONS) {
                CustomerLogger.w("ImageFilePicker", "Unsupported file type: $extension for file: $fileName")
                return null
            }

            // Validate file size
            if (fileSize > MAX_FILE_SIZE) {
                CustomerLogger.w("ImageFilePicker", "File too large: ${fileSize}B for file: $fileName (max: ${MAX_FILE_SIZE}B)")
                return null
            }

            // Determine content type
            val contentType = when (extension) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "bmp" -> "image/bmp"
                else -> "image/*"
            }

            // Read file data
            val imageData = file.readBytes()

            ImageFilePickerResult(
                fileName = fileName,
                contentType = contentType,
                fileSize = fileSize,
                imageData = imageData
            )
        } catch (e: Exception) {
            CustomerLogger.e("ImageFilePicker", "Error processing picked file: ${file.name}", e)
            null
        }
    }
}