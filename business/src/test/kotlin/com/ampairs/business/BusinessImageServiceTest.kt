package com.ampairs.business

import com.ampairs.business.exception.BusinessImageNotFoundException
import com.ampairs.business.exception.BusinessImageValidationException
import com.ampairs.business.model.Business
import com.ampairs.business.model.BusinessImage
import com.ampairs.business.model.BusinessImageType
import com.ampairs.business.model.enums.BusinessType
import com.ampairs.business.repository.BusinessImageRepository
import com.ampairs.business.repository.BusinessRepository
import com.ampairs.file.config.StorageProperties
import com.ampairs.file.service.ImageResizingService
import com.ampairs.file.storage.ObjectStorageService
import com.ampairs.file.storage.ObjectSummary
import com.ampairs.file.storage.UploadResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
import org.springframework.web.multipart.MultipartFile
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BusinessImageServiceTest {

    @Mock
    private lateinit var objectStorageService: ObjectStorageService

    @Mock
    private lateinit var imageResizingService: ImageResizingService

    @Mock
    private lateinit var businessRepository: BusinessRepository

    @Mock
    private lateinit var businessImageRepository: BusinessImageRepository

    private val storageProperties = StorageProperties()
    private lateinit var service: com.ampairs.business.service.BusinessImageService

    private fun business(): Business = Business().apply {
        uid = "BIZ-1"
        ownerId = "WSP-1"
        name = "Acme"
        businessType = BusinessType.RETAIL
    }

    private fun pngBytes(): ByteArray {
        val image = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "png", out)
        return out.toByteArray()
    }

    private fun pngFile(): MultipartFile =
        MockMultipartFile("file", "pic.png", "image/png", pngBytes())

    @BeforeEach
    fun setUp() {
        service = com.ampairs.business.service.BusinessImageService(
            objectStorageService, imageResizingService, storageProperties,
            businessRepository, businessImageRepository,
        )
        whenever(businessRepository.save(any<Business>())).thenAnswer { it.arguments[0] }
        whenever(businessImageRepository.save(any<BusinessImage>())).thenAnswer { it.arguments[0] }
        whenever(objectStorageService.uploadFile(any<ByteArray>(), any(), any(), any(), any()))
            .thenReturn(UploadResult("k", "etag", 1L, null, null))
        whenever(objectStorageService.listObjects(any(), any(), any())).thenReturn(emptyList())
        whenever(imageResizingService.resizeImage(any(), any(), any(), any(), any()))
            .thenReturn(byteArrayOf(1, 2, 3))
        whenever(imageResizingService.getImageDimensions(any())).thenReturn(Pair(800, 600))
    }

    @Test
    fun `uploadLogo stores keys and saves the business`() {
        val biz = business()

        val result = service.uploadLogo(biz, pngFile())

        assertNotNull(result.logoUrl)
        assertNotNull(result.logoThumbnailUrl)
        assertNotNull(result.logoUpdatedAt)
        verify(businessRepository).save(biz)
    }

    @Test
    fun `uploadLogo rejects an empty file`() {
        val empty = MockMultipartFile("file", "pic.png", "image/png", ByteArray(0))
        assertThrows(BusinessImageValidationException::class.java) {
            service.uploadLogo(business(), empty)
        }
    }

    @Test
    fun `uploadLogo rejects an invalid content type`() {
        val txt = MockMultipartFile("file", "f.txt", "text/plain", pngBytes())
        assertThrows(BusinessImageValidationException::class.java) {
            service.uploadLogo(business(), txt)
        }
    }

    @Test
    fun `getLogo returns bytes or throws not found`() {
        whenever(objectStorageService.downloadFile(any(), eq("k")))
            .thenReturn(ByteArrayInputStream(byteArrayOf(9, 8, 7)))
        assertEquals(3, service.getLogo("k").size)

        whenever(objectStorageService.downloadFile(any(), eq("bad"))).thenThrow(RuntimeException("x"))
        assertThrows(BusinessImageNotFoundException::class.java) { service.getLogo("bad") }
    }

    @Test
    fun `deleteLogo clears keys`() {
        val biz = business().apply { logoUrl = "k1"; logoThumbnailUrl = "k2" }
        val result = service.deleteLogo(biz)
        assertEquals(null, result.logoUrl)
        assertEquals(null, result.logoThumbnailUrl)
    }

    @Test
    fun `uploadImage creates a gallery image record`() {
        val biz = business()
        whenever(businessImageRepository.countByBusinessIdAndActive("BIZ-1", true)).thenReturn(0)
        whenever(businessImageRepository.getMaxDisplayOrder("BIZ-1")).thenReturn(0)

        val saved = service.uploadImage(biz, pngFile(), isPrimary = true, title = "Front")

        assertEquals("BIZ-1", saved.businessId)
        assertTrue(saved.isPrimary)
        assertEquals("Front", saved.title)
        verify(businessImageRepository).clearAllPrimaryFlags("BIZ-1")
        verify(businessImageRepository).save(any<BusinessImage>())
    }

    @Test
    fun `uploadImage enforces the per-business limit`() {
        whenever(businessImageRepository.countByBusinessIdAndActive("BIZ-1", true)).thenReturn(20)

        assertThrows(BusinessImageValidationException::class.java) {
            service.uploadImage(business(), pngFile())
        }
        verify(businessImageRepository, never()).save(any<BusinessImage>())
    }

    @Test
    fun `getImageByUid returns or throws`() {
        val img = BusinessImage().apply { uid = "IMG-1"; businessId = "BIZ-1"; imageUrl = "k" }
        whenever(businessImageRepository.findByUid("IMG-1")).thenReturn(img)
        assertSame(img, service.getImageByUid("IMG-1"))

        whenever(businessImageRepository.findByUid("none")).thenReturn(null)
        assertThrows(BusinessImageNotFoundException::class.java) { service.getImageByUid("none") }
    }

    @Test
    fun `getImageBytes throws not found on failure`() {
        whenever(objectStorageService.downloadFile(any(), any())).thenThrow(RuntimeException("x"))
        assertThrows(BusinessImageNotFoundException::class.java) { service.getImageBytes("k") }
    }

    @Test
    fun `updateImage applies provided fields`() {
        val img = BusinessImage().apply { uid = "IMG-1"; businessId = "BIZ-1"; imageUrl = "k" }
        whenever(businessImageRepository.findByUid("IMG-1")).thenReturn(img)

        val result = service.updateImage("IMG-1", title = "New", description = "Desc", imageType = BusinessImageType.BANNER)

        assertEquals("New", result.title)
        assertEquals("Desc", result.description)
        assertEquals(BusinessImageType.BANNER, result.imageType)
    }

    @Test
    fun `setAsPrimary clears others and flags this image`() {
        val img = BusinessImage().apply { uid = "IMG-1"; businessId = "BIZ-1"; imageUrl = "k" }
        whenever(businessImageRepository.findByUid("IMG-1")).thenReturn(img)

        val result = service.setAsPrimary("IMG-1")

        assertTrue(result.isPrimary)
        verify(businessImageRepository).clearPrimaryFlagExcept("BIZ-1", "IMG-1")
    }

    @Test
    fun `reorderImages updates display order for matching images`() {
        val a = BusinessImage().apply { uid = "A"; businessId = "BIZ-1"; imageUrl = "k" }
        val b = BusinessImage().apply { uid = "B"; businessId = "BIZ-1"; imageUrl = "k" }
        val other = BusinessImage().apply { uid = "C"; businessId = "OTHER"; imageUrl = "k" }
        whenever(businessImageRepository.findByUid("A")).thenReturn(a)
        whenever(businessImageRepository.findByUid("B")).thenReturn(b)
        whenever(businessImageRepository.findByUid("C")).thenReturn(other)

        service.reorderImages("BIZ-1", listOf("A", "B", "C"))

        assertEquals(0, a.displayOrder)
        assertEquals(1, b.displayOrder)
        verify(businessImageRepository).save(a)
        verify(businessImageRepository).save(b)
        verify(businessImageRepository, never()).save(other)
    }

    @Test
    fun `deleteImage removes from storage and database`() {
        val img = BusinessImage().apply { uid = "IMG-1"; businessId = "BIZ-1"; imageUrl = "full"; thumbnailUrl = "thumb" }
        whenever(businessImageRepository.findByUid("IMG-1")).thenReturn(img)

        val msg = service.deleteImage("IMG-1")

        assertTrue(msg.contains("deleted"))
        verify(objectStorageService).deleteObject(any(), eq("full"))
        verify(objectStorageService).deleteObject(any(), eq("thumb"))
        verify(businessImageRepository).delete(img)
    }

    @Test
    fun `deleteAllImages cleans storage and deletes by business`() {
        val img = BusinessImage().apply { uid = "IMG-1"; businessId = "BIZ-1"; imageUrl = "full"; thumbnailUrl = "thumb" }
        whenever(businessImageRepository.findByBusinessIdAndActiveOrderByDisplayOrderAsc("BIZ-1", true))
            .thenReturn(listOf(img))

        service.deleteAllImages("BIZ-1")

        verify(objectStorageService).deleteObject(any(), eq("full"))
        verify(businessImageRepository).deleteByBusinessId("BIZ-1")
    }

    @Test
    fun `getPrimaryImage and getBusinessImages delegate to repository`() {
        val img = BusinessImage().apply { uid = "IMG-1"; businessId = "BIZ-1"; imageUrl = "k" }
        whenever(businessImageRepository.findByBusinessIdAndIsPrimaryTrueAndActiveTrue("BIZ-1")).thenReturn(img)
        whenever(businessImageRepository.findByBusinessIdAndActiveOrderByDisplayOrderAsc("BIZ-1", true))
            .thenReturn(listOf(img))
        whenever(businessImageRepository.findByBusinessIdAndImageTypeAndActiveOrderByDisplayOrderAsc("BIZ-1", BusinessImageType.GALLERY, true))
            .thenReturn(listOf(img))

        assertSame(img, service.getPrimaryImage("BIZ-1"))
        assertEquals(1, service.getBusinessImages("BIZ-1").size)
        assertEquals(1, service.getBusinessImagesByType("BIZ-1", BusinessImageType.GALLERY).size)
    }
}
