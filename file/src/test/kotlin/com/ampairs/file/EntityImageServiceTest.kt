package com.ampairs.file

import com.ampairs.core.exception.NotFoundException
import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.file.config.StorageProperties
import com.ampairs.file.domain.dto.EntityImageReorderRequest
import com.ampairs.file.domain.dto.EntityImageUpdateRequest
import com.ampairs.file.domain.dto.EntityImageUploadRequest
import com.ampairs.file.domain.model.EntityImage
import com.ampairs.file.domain.model.EntityImageMetadata
import com.ampairs.file.domain.service.EntityImageService
import com.ampairs.file.repository.EntityImageRepository
import com.ampairs.file.service.FileValidationService
import com.ampairs.file.service.ImageResizingService
import com.ampairs.file.service.ProcessedImage
import com.ampairs.file.service.ThumbnailCacheService
import com.ampairs.file.storage.ObjectStorageService
import com.ampairs.file.storage.UploadResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.mock.web.MockMultipartFile
import java.io.ByteArrayInputStream
import java.time.Instant

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EntityImageServiceTest {

    @Mock private lateinit var entityImageRepository: EntityImageRepository
    @Mock private lateinit var objectStorageService: ObjectStorageService
    @Mock private lateinit var thumbnailCacheService: ThumbnailCacheService
    @Mock private lateinit var imageResizingService: ImageResizingService
    @Mock private lateinit var fileValidationService: FileValidationService
    @Mock private lateinit var entityChangePublisher: EntityChangePublisher

    private val storageProperties = StorageProperties()
    private lateinit var service: EntityImageService

    private val baseUrl = "/file/v1"

    private fun image(block: EntityImage.() -> Unit = {}): EntityImage =
        EntityImage().apply {
            uid = "EIM-1"
            entityType = "customer"
            entityUid = "CUS-1"
            originalFilename = "pic.jpg"
            fileExtension = "jpg"
            contentType = "image/jpeg"
            fileSize = 1000
            checksum = "sum"
            storagePath = "WSP/customer/CUS-1/EIM-1.jpg"
            metadata = EntityImageMetadata(width = 10, height = 10)
            active = true
            block()
        }

    @BeforeEach
    fun setUp() {
        service = EntityImageService(
            entityImageRepository, objectStorageService, thumbnailCacheService,
            imageResizingService, fileValidationService, storageProperties, entityChangePublisher,
        )
        whenever(entityImageRepository.save(any<EntityImage>())).thenAnswer { it.arguments[0] }
        whenever(objectStorageService.uploadFile(any<ByteArray>(), any(), any(), any(), any()))
            .thenReturn(UploadResult("k", "etag", 1L, Instant.now(), "http://url"))
    }

    private fun validImage() {
        whenever(fileValidationService.validateFile(any(), any(), anyNull(), any()))
            .thenReturn(FileValidationService.FileValidationResult(true, emptyList(), "pic.jpg", 100, "sum"))
    }

    // mockito-kotlin has no anyNull for nullable Long?; use isNull-friendly matcher
    private fun anyNull(): Long? = org.mockito.kotlin.anyOrNull()

    @Test
    fun `upload validates, processes, stores and persists a new image`() {
        validImage()
        whenever(entityImageRepository.findByChecksum("sum", "customer", "CUS-1")).thenReturn(null)
        whenever(imageResizingService.processForStorage(any(), any()))
            .thenReturn(ProcessedImage(byteArrayOf(1, 2, 3), 64, 48))
        whenever(entityImageRepository.getNextDisplayOrder("customer", "CUS-1")).thenReturn(2)

        val file = MockMultipartFile("file", "pic.jpg", "image/jpeg", byteArrayOf(9))
        val resp = service.upload("customer", "CUS-1", file, EntityImageUploadRequest("customer", "CUS-1"), baseUrl)

        assertEquals("customer", resp.entityType)
        assertEquals(2, resp.displayOrder)
        verify(entityImageRepository).save(any<EntityImage>())
        verify(entityChangePublisher).created(eq("customer_image"), eq("CUS-1"))
    }

    @Test
    fun `upload marks primary and clears existing primary`() {
        validImage()
        whenever(entityImageRepository.findByChecksum(any(), any(), any())).thenReturn(null)
        whenever(imageResizingService.processForStorage(any(), any()))
            .thenReturn(ProcessedImage(byteArrayOf(1), 64, 48))

        val file = MockMultipartFile("file", "pic.jpg", "image/jpeg", byteArrayOf(9))
        val resp = service.upload(
            "customer", "CUS-1", file,
            EntityImageUploadRequest("customer", "CUS-1", isPrimary = true), baseUrl,
        )

        assertTrue(resp.isPrimary)
        assertEquals(0, resp.displayOrder)
        verify(entityImageRepository).clearPrimaryStatus("customer", "CUS-1")
    }

    @Test
    fun `upload returns existing image on dedup hit`() {
        validImage()
        val existing = image()
        whenever(entityImageRepository.findByChecksum("sum", "customer", "CUS-1")).thenReturn(existing)

        val file = MockMultipartFile("file", "pic.jpg", "image/jpeg", byteArrayOf(9))
        val resp = service.upload("customer", "CUS-1", file, EntityImageUploadRequest("customer", "CUS-1"), baseUrl)

        assertEquals("EIM-1", resp.uid)
        verify(entityImageRepository, never()).save(any<EntityImage>())
        verify(imageResizingService, never()).processForStorage(any(), any())
    }

    @Test
    fun `upload rejects an invalid image`() {
        whenever(fileValidationService.validateFile(any(), any(), anyNull(), any()))
            .thenReturn(FileValidationService.FileValidationResult(false, listOf("bad")))

        val file = MockMultipartFile("file", "pic.jpg", "image/jpeg", byteArrayOf(9))
        assertThrows(IllegalArgumentException::class.java) {
            service.upload("customer", "CUS-1", file, EntityImageUploadRequest("customer", "CUS-1"), baseUrl)
        }
    }

    @Test
    fun `upload routes document entity types through the document path`() {
        whenever(fileValidationService.validateFile(any(), any(), anyNull(), any()))
            .thenReturn(FileValidationService.FileValidationResult(true, emptyList(), "tpl.html", 50, "sum"))
        whenever(entityImageRepository.findByChecksum(any(), any(), any())).thenReturn(null)
        whenever(entityImageRepository.getNextDisplayOrder(any(), any())).thenReturn(0)

        val file = MockMultipartFile("file", "tpl.html", "text/html", "<html></html>".toByteArray())
        val resp = service.upload(
            "PRINT_TEMPLATE", "PT-1", file,
            EntityImageUploadRequest("PRINT_TEMPLATE", "PT-1"), baseUrl,
        )

        assertEquals("PRINT_TEMPLATE", resp.entityType)
        // document path never invokes image processing
        verify(imageResizingService, never()).processForStorage(any(), any())
    }

    @Test
    fun `getImages returns active list response`() {
        whenever(entityImageRepository.findActiveByEntity("customer", "CUS-1")).thenReturn(listOf(image()))
        val list = service.getImages("customer", "CUS-1", baseUrl)
        assertEquals(1, list.totalCount)
    }

    @Test
    fun `getImage returns the active image`() {
        whenever(entityImageRepository.findByUidAndEntity("EIM-1", "customer", "CUS-1")).thenReturn(image())
        assertEquals("EIM-1", service.getImage("customer", "CUS-1", "EIM-1", baseUrl).uid)
    }

    @Test
    fun `getImage throws when image not found`() {
        whenever(entityImageRepository.findByUidAndEntity(any(), any(), any())).thenReturn(null)
        assertThrows(NotFoundException::class.java) { service.getImage("customer", "CUS-1", "x", baseUrl) }
    }

    @Test
    fun `getImage throws when image inactive`() {
        whenever(entityImageRepository.findByUidAndEntity("EIM-1", "customer", "CUS-1"))
            .thenReturn(image { active = false })
        assertThrows(NotFoundException::class.java) { service.getImage("customer", "CUS-1", "EIM-1", baseUrl) }
    }

    @Test
    fun `download returns image and stream`() {
        whenever(entityImageRepository.findByUidAndEntity("EIM-1", "customer", "CUS-1")).thenReturn(image())
        whenever(objectStorageService.downloadFile(any(), any())).thenReturn(ByteArrayInputStream(byteArrayOf(1)))

        val (img, _) = service.download("customer", "CUS-1", "EIM-1")
        assertEquals("EIM-1", img.uid)
    }

    @Test
    fun `downloadByUid throws when missing or inactive`() {
        whenever(entityImageRepository.findByUid("x")).thenReturn(null)
        assertThrows(NotFoundException::class.java) { service.downloadByUid("x") }

        whenever(entityImageRepository.findByUid("EIM-1")).thenReturn(image { active = false })
        assertThrows(NotFoundException::class.java) { service.downloadByUid("EIM-1") }
    }

    @Test
    fun `update applies description, order and primary`() {
        val img = image()
        whenever(entityImageRepository.findByUidAndEntity("EIM-1", "customer", "CUS-1")).thenReturn(img)

        val resp = service.update(
            "customer", "CUS-1", "EIM-1",
            EntityImageUpdateRequest(description = "new", displayOrder = 5, isPrimary = true), baseUrl,
        )

        assertEquals("new", resp.description)
        assertTrue(resp.isPrimary)
        assertEquals(0, resp.displayOrder) // primary forces order 0
        verify(entityImageRepository).clearPrimaryStatus("customer", "CUS-1")
        verify(entityChangePublisher).updated(eq("customer_image"), eq("CUS-1"))
    }

    @Test
    fun `update can clear primary flag`() {
        val img = image { isPrimary = true }
        whenever(entityImageRepository.findByUidAndEntity("EIM-1", "customer", "CUS-1")).thenReturn(img)

        val resp = service.update(
            "customer", "CUS-1", "EIM-1",
            EntityImageUpdateRequest(isPrimary = false), baseUrl,
        )
        assertFalse(resp.isPrimary)
    }

    @Test
    fun `setPrimary flags this image and clears others`() {
        val img = image()
        whenever(entityImageRepository.findByUidAndEntity("EIM-1", "customer", "CUS-1")).thenReturn(img)

        val resp = service.setPrimary("customer", "CUS-1", "EIM-1", baseUrl)
        assertTrue(resp.isPrimary)
        verify(entityImageRepository).clearPrimaryStatus("customer", "CUS-1")
    }

    @Test
    fun `delete soft deletes and cleans storage`() {
        val img = image()
        whenever(entityImageRepository.findByUidAndEntity("EIM-1", "customer", "CUS-1")).thenReturn(img)

        val ok = service.delete("customer", "CUS-1", "EIM-1")

        assertTrue(ok)
        verify(entityImageRepository).softDelete("EIM-1")
        verify(objectStorageService).deleteObject(any(), eq(img.storagePath))
        verify(entityChangePublisher).deleted(eq("customer_image"), eq("CUS-1"))
    }

    @Test
    fun `delete throws when image not found`() {
        whenever(entityImageRepository.findByUidAndEntity(any(), any(), any())).thenReturn(null)
        assertThrows(NotFoundException::class.java) { service.delete("customer", "CUS-1", "x") }
    }

    @Test
    fun `delete returns false when storage cleanup fails`() {
        val img = image()
        whenever(entityImageRepository.findByUidAndEntity("EIM-1", "customer", "CUS-1")).thenReturn(img)
        whenever(objectStorageService.deleteObject(any(), any())).thenThrow(RuntimeException("x"))

        assertFalse(service.delete("customer", "CUS-1", "EIM-1"))
    }

    @Test
    fun `deleteByUid is idempotent for inactive images`() {
        whenever(entityImageRepository.findByUid("EIM-1")).thenReturn(image { active = false })
        service.deleteByUid("EIM-1")
        verify(entityImageRepository, never()).softDelete(any())
    }

    @Test
    fun `deleteByUid soft deletes active image`() {
        whenever(entityImageRepository.findByUid("EIM-1")).thenReturn(image())
        service.deleteByUid("EIM-1")
        verify(entityImageRepository).softDelete("EIM-1")
        verify(entityChangePublisher).deleted(eq("customer_image"), eq("CUS-1"))
    }

    @Test
    fun `bulkDelete aggregates successes and failures`() {
        val a = image { uid = "A" }
        whenever(entityImageRepository.findByUidAndEntity("A", "customer", "CUS-1")).thenReturn(a)
        whenever(entityImageRepository.findByUidAndEntity("B", "customer", "CUS-1")).thenReturn(null)

        val result = service.bulkDelete("customer", "CUS-1", listOf("A", "B"))

        assertEquals(1, result.successCount)
        assertEquals(1, result.failureCount)
        assertTrue(result.successfulImageUids.contains("A"))
        assertTrue(result.failedImageUids.contains("B"))
    }

    @Test
    fun `reorder updates display order and throws on missing`() {
        val a = image { uid = "A" }
        whenever(entityImageRepository.findByUidAndEntity("A", "customer", "CUS-1")).thenReturn(a)
        whenever(entityImageRepository.findActiveByEntity("customer", "CUS-1")).thenReturn(listOf(a))

        service.reorder(
            "customer", "CUS-1",
            listOf(EntityImageReorderRequest.ImageOrderItem("A", 3)), baseUrl,
        )
        verify(entityImageRepository).updateDisplayOrder("A", 3)

        whenever(entityImageRepository.findByUidAndEntity("Z", "customer", "CUS-1")).thenReturn(null)
        assertThrows(NotFoundException::class.java) {
            service.reorder(
                "customer", "CUS-1",
                listOf(EntityImageReorderRequest.ImageOrderItem("Z", 1)), baseUrl,
            )
        }
    }

    @Test
    fun `stats returns zeros when no images`() {
        whenever(entityImageRepository.findActiveByEntity("customer", "CUS-1")).thenReturn(emptyList())
        val stats = service.stats("customer", "CUS-1", baseUrl)
        assertEquals(0, stats.totalImages)
        assertEquals(0, stats.totalSize)
    }

    @Test
    fun `stats aggregates totals for images`() {
        val a = image { uid = "A"; fileSize = 1000; uploadedAt = Instant.now().minusSeconds(100) }
        val b = image { uid = "B"; fileSize = 3000; isPrimary = true; uploadedAt = Instant.now() }
        whenever(entityImageRepository.findActiveByEntity("customer", "CUS-1")).thenReturn(listOf(a, b))

        val stats = service.stats("customer", "CUS-1", baseUrl)

        assertEquals(2, stats.totalImages)
        assertEquals(1, stats.primaryImages)
        assertEquals(4000, stats.totalSize)
        assertEquals(2000, stats.averageSize)
        assertEquals("B", stats.largestImage!!.uid)
        assertEquals("A", stats.oldestImage!!.uid)
        assertEquals("B", stats.newestImage!!.uid)
    }
}
