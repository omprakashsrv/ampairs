package com.ampairs.file

import com.ampairs.file.config.StorageProperties
import com.ampairs.file.domain.model.File
import com.ampairs.file.domain.service.FileAccessException
import com.ampairs.file.domain.service.FileNotFoundException
import com.ampairs.file.domain.service.FileService
import com.ampairs.file.domain.service.FileUploadException
import com.ampairs.file.repository.FileRepository
import com.ampairs.file.storage.UploadResult
import com.ampairs.file.storage.ObjectStorageService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileServiceTest {

    @Mock
    private lateinit var objectStorageService: ObjectStorageService

    @Mock
    private lateinit var fileRepository: FileRepository

    private val storageProperties = StorageProperties()
    private lateinit var service: FileService

    @BeforeEach
    fun setUp() {
        service = FileService(objectStorageService, fileRepository, storageProperties)
        whenever(fileRepository.save(any<File>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `saveFile uploads bytes and persists the record`() {
        whenever(objectStorageService.uploadFile(any<ByteArray>(), any(), any(), any(), any()))
            .thenReturn(UploadResult("key", "etag-123", 3L, null, null))

        val bytes = byteArrayOf(1, 2, 3)
        val file = service.saveFile(bytes, "report.txt", "text/plain")

        assertEquals("report.txt", file.name)
        assertEquals("etag-123", file.etag)
        assertEquals(3L, file.size)
        assertEquals(storageProperties.defaultBucket, file.bucket)
        assertTrue(file.objectKey.contains("report.txt"))
        assertTrue(file.objectKey.startsWith("uploads/"))
    }

    @Test
    fun `saveFile honours explicit folder and bucket`() {
        whenever(objectStorageService.uploadFile(any<ByteArray>(), any(), any(), any(), any()))
            .thenReturn(UploadResult("key", null, 1L, null, null))

        val file = service.saveFile(byteArrayOf(1), "n.png", "image/png", folder = "logos", bucket = "mybucket")

        assertEquals("mybucket", file.bucket)
        assertTrue(file.objectKey.startsWith("logos/"))
        assertEquals("", file.etag) // null etag stored as empty string
    }

    @Test
    fun `saveFile wraps storage failures`() {
        whenever(objectStorageService.uploadFile(any<ByteArray>(), any(), any(), any(), any()))
            .thenThrow(RuntimeException("s3 down"))

        assertThrows(FileUploadException::class.java) {
            service.saveFile(byteArrayOf(1), "n.png", "image/png")
        }
    }

    @Test
    fun `getFile returns or throws not found`() {
        val f = File().apply { uid = "FIL-1"; name = "x" }
        whenever(fileRepository.findByUid("FIL-1")).thenReturn(Optional.of(f))
        assertSame(f, service.getFile("FIL-1"))

        whenever(fileRepository.findByUid("none")).thenReturn(Optional.empty())
        assertThrows(FileNotFoundException::class.java) { service.getFile("none") }
    }

    @Test
    fun `getFileUrl returns a presigned url`() {
        val f = File().apply { uid = "FIL-1"; bucket = "b"; objectKey = "k" }
        whenever(fileRepository.findByUid("FIL-1")).thenReturn(Optional.of(f))
        whenever(objectStorageService.generatePresignedUrl("b", "k", 60 * 60))
            .thenReturn("https://signed/url")

        assertEquals("https://signed/url", service.getFileUrl("FIL-1"))
    }

    @Test
    fun `getFileUrl throws not found when file missing`() {
        whenever(fileRepository.findByUid("none")).thenReturn(Optional.empty())
        assertThrows(FileNotFoundException::class.java) { service.getFileUrl("none") }
    }

    @Test
    fun `getFileUrl wraps presign errors`() {
        val f = File().apply { uid = "FIL-1"; bucket = "b"; objectKey = "k" }
        whenever(fileRepository.findByUid("FIL-1")).thenReturn(Optional.of(f))
        whenever(objectStorageService.generatePresignedUrl(any(), any(), any()))
            .thenThrow(RuntimeException("boom"))

        assertThrows(FileAccessException::class.java) { service.getFileUrl("FIL-1") }
    }
}
