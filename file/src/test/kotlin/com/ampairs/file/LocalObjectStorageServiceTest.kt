package com.ampairs.file

import com.ampairs.file.config.StorageProperties
import com.ampairs.file.storage.InvalidObjectKeyException
import com.ampairs.file.storage.LocalObjectStorageService
import com.ampairs.file.storage.ObjectNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class LocalObjectStorageServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var service: LocalObjectStorageService
    private val bucket = "test-bucket"

    @BeforeEach
    fun setUp() {
        val props = StorageProperties(
            local = StorageProperties.LocalConfig(basePath = tempDir.toString())
        )
        service = LocalObjectStorageService(props)
    }

    @Test
    fun `upload then download round-trips bytes`() {
        val result = service.uploadFile(byteArrayOf(1, 2, 3), bucket, "dir/file.bin", "application/octet-stream")
        assertEquals("dir/file.bin", result.objectKey)
        assertEquals(3L, result.contentLength)

        val bytes = service.downloadFile(bucket, "dir/file.bin").readBytes()
        assertEquals(3, bytes.size)
    }

    @Test
    fun `download throws when missing`() {
        assertThrows(ObjectNotFoundException::class.java) { service.downloadFile(bucket, "nope.bin") }
    }

    @Test
    fun `objectExists reflects presence`() {
        assertFalse(service.objectExists(bucket, "a.bin"))
        service.uploadFile(byteArrayOf(1), bucket, "a.bin", "application/octet-stream")
        assertTrue(service.objectExists(bucket, "a.bin"))
    }

    @Test
    fun `getObjectMetadata returns size and etag`() {
        service.uploadFile(byteArrayOf(1, 2, 3, 4), bucket, "m.bin", "application/octet-stream")
        val meta = service.getObjectMetadata(bucket, "m.bin")
        assertEquals(4L, meta.contentLength)
        assertNotNull(meta.etag)
    }

    @Test
    fun `getObjectMetadata throws when missing`() {
        assertThrows(ObjectNotFoundException::class.java) { service.getObjectMetadata(bucket, "ghost.bin") }
    }

    @Test
    fun `deleteObject removes the file`() {
        service.uploadFile(byteArrayOf(1), bucket, "del.bin", "application/octet-stream")
        service.deleteObject(bucket, "del.bin")
        assertFalse(service.objectExists(bucket, "del.bin"))
    }

    @Test
    fun `copyObject duplicates the file`() {
        service.uploadFile(byteArrayOf(1, 2), bucket, "src.bin", "application/octet-stream")
        val result = service.copyObject(bucket, "src.bin", bucket, "dst.bin")
        assertEquals("dst.bin", result.targetKey)
        assertTrue(service.objectExists(bucket, "dst.bin"))
    }

    @Test
    fun `listObjects returns stored files under prefix`() {
        service.uploadFile(byteArrayOf(1), bucket, "folder/one.bin", "application/octet-stream")
        service.uploadFile(byteArrayOf(2), bucket, "folder/two.bin", "application/octet-stream")

        val list = service.listObjects(bucket, "folder/x")
        assertEquals(2, list.size)
    }

    @Test
    fun `listObjects returns empty for unknown prefix`() {
        assertTrue(service.listObjects(bucket, "missing/x").isEmpty())
    }

    @Test
    fun `generatePresignedUrl builds a local url`() {
        val url = service.generatePresignedUrl(bucket, "a/b.bin")
        assertTrue(url.contains(bucket))
        assertTrue(url.endsWith("a/b.bin"))
    }

    @Test
    fun `createBucketIfNotExists creates directory`() {
        service.createBucketIfNotExists("new-bucket")
        assertTrue(tempDir.resolve("new-bucket").toFile().isDirectory)
    }

    @Test
    fun `object key validation rejects traversal and blanks`() {
        // generatePresignedUrl validates the key without wrapping the exception
        assertThrows(InvalidObjectKeyException::class.java) { service.generatePresignedUrl(bucket, "../escape.bin") }
        assertThrows(InvalidObjectKeyException::class.java) { service.generatePresignedUrl(bucket, "") }
        assertThrows(InvalidObjectKeyException::class.java) { service.generatePresignedUrl(bucket, "/abs.bin") }
        assertThrows(InvalidObjectKeyException::class.java) { service.generatePresignedUrl(bucket, "a".repeat(1100)) }
    }

    @Test
    fun `generateWorkspaceKey from interface default`() {
        assertEquals("ws/customer/CUS-1/F-1.jpg", service.generateWorkspaceKey("ws", "customer", "CUS-1", "F-1", "jpg"))
    }
}
