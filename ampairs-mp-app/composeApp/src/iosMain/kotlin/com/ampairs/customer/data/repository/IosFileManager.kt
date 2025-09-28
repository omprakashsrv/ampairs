package com.ampairs.customer.data.repository

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.writeToFile
import platform.Foundation.memcpy

/**
 * iOS-specific implementation of PlatformFileManager.
 * Handles file operations using iOS's Documents directory for persistent storage
 * and Caches directory for temporary storage.
 */
@OptIn(ExperimentalForeignApi::class)
class IosFileManager : PlatformFileManager {

    override suspend fun saveImageToCache(imageId: String, imageData: ByteArray, fileName: String): String =
        withContext(Dispatchers.Default) { // iOS doesn't have IO dispatcher
            val cacheDir = getCacheDirectory()
            val fileExtension = fileName.substringAfterLast('.', "")
            val cacheFileName = if (fileExtension.isNotEmpty()) {
                "${imageId}.$fileExtension"
            } else {
                imageId
            }

            val filePath = "$cacheDir/$cacheFileName"

            // Create NSData from ByteArray and write to file
            val nsData = imageData.toNSData()
            nsData.writeToFile(filePath, atomically = true)

            filePath
        }

    override suspend fun deleteFile(filePath: String): Unit = withContext(Dispatchers.Default) {
        val fileManager = NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(filePath)) {
            fileManager.removeItemAtPath(filePath, error = null)
        }
    }

    override suspend fun fileExists(filePath: String): Boolean = withContext(Dispatchers.Default) {
        NSFileManager.defaultManager.fileExistsAtPath(filePath)
    }

    override suspend fun getFileSize(filePath: String): Long = withContext(Dispatchers.Default) {
        val fileManager = NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(filePath)) {
            val attributes = fileManager.attributesOfItemAtPath(filePath, error = null)
            attributes?.get("NSFileSize") as? Long ?: 0L
        } else {
            0L
        }
    }

    override suspend fun getCacheDirectory(): String = withContext(Dispatchers.Default) {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSCachesDirectory,
            NSUserDomainMask,
            true
        )
        val cachesPath = paths.first() as String
        val customerImagesDir = "$cachesPath/customer_images"

        // Create directory if it doesn't exist
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(customerImagesDir)) {
            fileManager.createDirectoryAtPath(
                customerImagesDir,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }

        customerImagesDir
    }

    override suspend fun readFile(filePath: String): ByteArray = withContext(Dispatchers.Default) {
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(filePath)) {
            throw IllegalArgumentException("File not found: $filePath")
        }

        val nsData = platform.Foundation.NSData.dataWithContentsOfFile(filePath)
            ?: throw IllegalArgumentException("Could not read file: $filePath")

        // Convert NSData to ByteArray
        nsData.toByteArray()
    }

    private fun ByteArray.toNSData(): platform.Foundation.NSData {
        return platform.Foundation.NSData.create(
            bytes = this.refTo(0),
            length = this.size.toULong()
        )
    }

    private fun platform.Foundation.NSData.toByteArray(): ByteArray {
        val length = this.length.toInt()
        val byteArray = ByteArray(length)
        if (length > 0) {
            byteArray.usePinned { pinned ->
                platform.Foundation.memcpy(pinned.addressOf(0), this.bytes, this.length)
            }
        }
        return byteArray
    }
}