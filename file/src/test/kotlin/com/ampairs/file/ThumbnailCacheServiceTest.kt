package com.ampairs.file

import com.ampairs.file.config.StorageProperties
import com.ampairs.file.service.ImageResizingService
import com.ampairs.file.service.ThumbnailCacheService
import com.ampairs.file.storage.ObjectMetadata
import com.ampairs.file.storage.ObjectStorageService
import com.ampairs.file.storage.ObjectSummary
import com.ampairs.file.storage.UploadResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import javax.imageio.ImageIO

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ThumbnailCacheServiceTest {

    @Mock private lateinit var objectStorageService: ObjectStorageService
    private val storageProperties = StorageProperties()
    private lateinit var imageResizingService: ImageResizingService
    private lateinit var service: ThumbnailCacheService

    private val bucket = "b"
    private val original = "ws/customer/CUS-1/img.jpg"

    private fun jpgBytes(): ByteArray {
        val image = BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB)
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", out)
        return out.toByteArray()
    }

    @BeforeEach
    fun setUp() {
        imageResizingService = ImageResizingService(storageProperties)
        service = ThumbnailCacheService(objectStorageService, imageResizingService, storageProperties)
        whenever(objectStorageService.uploadFile(any<ByteArray>(), any(), any(), any(), any()))
            .thenReturn(UploadResult("k", "etag", 1L, Instant.now(), null))
    }

    @Test
    fun `getThumbnail returns original when thumbnails disabled`() {
        val props = StorageProperties(image = StorageProperties.ImageConfig(
            thumbnails = StorageProperties.ThumbnailConfig(enabled = false)
        ))
        val svc = ThumbnailCacheService(objectStorageService, imageResizingService, props)
        whenever(objectStorageService.downloadFile(bucket, original)).thenReturn(ByteArrayInputStream(byteArrayOf(1)))

        val (stream, cached) = svc.getThumbnail(bucket, original, ImageResizingService.ThumbnailSize.SMALL)
        assertNotNull(stream)
        assertFalse(cached)
    }

    @Test
    fun `getThumbnail serves cached thumbnail when it exists`() {
        whenever(objectStorageService.objectExists(eq(bucket), any())).thenReturn(true)
        whenever(objectStorageService.downloadFile(eq(bucket), any())).thenReturn(ByteArrayInputStream(byteArrayOf(7)))

        val (_, cached) = service.getThumbnail(bucket, original, ImageResizingService.ThumbnailSize.MEDIUM)
        assertTrue(cached)
    }

    @Test
    fun `getThumbnail generates and caches when not present`() {
        whenever(objectStorageService.objectExists(eq(bucket), any())).thenReturn(false)
        whenever(objectStorageService.downloadFile(eq(bucket), eq(original))).thenReturn(ByteArrayInputStream(jpgBytes()))

        val (stream, cached) = service.getThumbnail(bucket, original, ImageResizingService.ThumbnailSize.SMALL)

        assertNotNull(stream)
        assertFalse(cached) // freshly generated, not served-from-cache
        verify(objectStorageService).uploadFile(any<ByteArray>(), eq(bucket), any(), any(), any())
    }

    @Test
    fun `getThumbnail falls back to original on error`() {
        whenever(objectStorageService.objectExists(eq(bucket), any())).thenReturn(false)
        // first download (for generation) fails, fallback download returns original
        whenever(objectStorageService.downloadFile(eq(bucket), eq(original)))
            .thenThrow(RuntimeException("boom"))
            .thenReturn(ByteArrayInputStream(byteArrayOf(1)))

        val (stream, cached) = service.getThumbnail(bucket, original, ImageResizingService.ThumbnailSize.SMALL)
        assertNotNull(stream)
        assertFalse(cached)
    }

    @Test
    fun `preGenerateThumbnails returns empty when autoGenerate disabled`() {
        // default autoGenerate is false
        val results = service.preGenerateThumbnails(bucket, original)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `preGenerateThumbnails generates for all sizes when enabled`() {
        val props = StorageProperties(image = StorageProperties.ImageConfig(
            thumbnails = StorageProperties.ThumbnailConfig(autoGenerate = true)
        ))
        val svc = ThumbnailCacheService(objectStorageService, imageResizingService, props)
        // fresh stream per call — one download per thumbnail size
        whenever(objectStorageService.downloadFile(eq(bucket), eq(original)))
            .thenAnswer { ByteArrayInputStream(jpgBytes()) }

        val results = svc.preGenerateThumbnails(bucket, original)
        assertEquals(3, results.size)
        assertTrue(results.all { it.success })
    }

    @Test
    fun `thumbnailExists checks storage when cache enabled`() {
        whenever(objectStorageService.objectExists(eq(bucket), any())).thenReturn(true)
        assertTrue(service.thumbnailExists(bucket, original, ImageResizingService.ThumbnailSize.SMALL))
    }

    @Test
    fun `deleteThumbnails removes existing thumbnails`() {
        whenever(objectStorageService.objectExists(eq(bucket), any())).thenReturn(true)

        val deleted = service.deleteThumbnails(bucket, original)
        assertEquals(3, deleted.size)
        verify(objectStorageService, org.mockito.kotlin.times(3)).deleteObject(eq(bucket), any())
    }

    @Test
    fun `getThumbnailMetadata returns metadata when present, null when absent`() {
        whenever(objectStorageService.objectExists(eq(bucket), any())).thenReturn(true)
        whenever(objectStorageService.getObjectMetadata(eq(bucket), any()))
            .thenReturn(ObjectMetadata("k", "image/jpeg", 100, "et", Instant.now()))

        val meta = service.getThumbnailMetadata(bucket, original, ImageResizingService.ThumbnailSize.SMALL)
        assertNotNull(meta)
        assertTrue(meta!!.cached)

        whenever(objectStorageService.objectExists(eq(bucket), any())).thenReturn(false)
        assertNull(service.getThumbnailMetadata(bucket, original, ImageResizingService.ThumbnailSize.SMALL))
    }

    @Test
    fun `cleanupOldThumbnails deletes objects older than cutoff`() {
        val old = ObjectSummary("thumbs/a_150.jpg", 10, Instant.now().minusSeconds(60L * 60 * 24 * 40), "e")
        val recent = ObjectSummary("thumbs/b_150.jpg", 10, Instant.now(), "e")
        whenever(objectStorageService.listObjects(eq(bucket), eq("thumbs/"), any())).thenReturn(listOf(old, recent))

        val count = service.cleanupOldThumbnails(bucket, 30)
        assertEquals(1, count)
        verify(objectStorageService).deleteObject(bucket, "thumbs/a_150.jpg")
        verify(objectStorageService, never()).deleteObject(bucket, "thumbs/b_150.jpg")
    }
}
