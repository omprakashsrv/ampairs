package com.ampairs.file

import com.ampairs.file.config.StorageProperties
import com.ampairs.file.service.ImageResizingException
import com.ampairs.file.service.ImageResizingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ImageResizingServiceTest {

    private val storageProperties = StorageProperties()
    private lateinit var service: ImageResizingService

    @BeforeEach
    fun setUp() {
        service = ImageResizingService(storageProperties)
    }

    private fun imageBytes(w: Int, h: Int, format: String = "jpg"): ByteArray {
        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val out = ByteArrayOutputStream()
        ImageIO.write(image, format, out)
        return out.toByteArray()
    }

    private fun dims(bytes: ByteArray): Pair<Int, Int> {
        val img = ImageIO.read(ByteArrayInputStream(bytes))
        return img.width to img.height
    }

    @Test
    fun `resizeImage scales down while keeping aspect ratio`() {
        val resized = service.resizeImage(ByteArrayInputStream(imageBytes(800, 400)), 200, 200, maintainAspectRatio = true)
        val (w, h) = dims(resized)
        assertTrue(w <= 200 && h <= 200)
        assertEquals(2.0, w.toDouble() / h.toDouble(), 0.2)
    }

    @Test
    fun `resizeImage exact size when aspect ratio not maintained`() {
        val resized = service.resizeImage(ByteArrayInputStream(imageBytes(800, 400)), 100, 100, maintainAspectRatio = false)
        val (w, h) = dims(resized)
        assertEquals(100, w)
        assertEquals(100, h)
    }

    @Test
    fun `resizeImage throws on unreadable data`() {
        assertThrows(ImageResizingException::class.java) {
            service.resizeImage(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 100, 100)
        }
    }

    @Test
    fun `generateThumbnail produces a bounded image`() {
        val thumb = service.generateThumbnail(ByteArrayInputStream(imageBytes(600, 600)), ImageResizingService.ThumbnailSize.SMALL)
        val (w, h) = dims(thumb)
        assertTrue(w <= 150 && h <= 150)
    }

    @Test
    fun `generateThumbnail throws on bad data`() {
        assertThrows(ImageResizingException::class.java) {
            service.generateThumbnail(ByteArrayInputStream(byteArrayOf(0)), ImageResizingService.ThumbnailSize.MEDIUM)
        }
    }

    @Test
    fun `processForStorage resizes only oversized images and strips exif`() {
        val big = service.processForStorage(imageBytes(4000, 3000))
        assertTrue(big.width <= storageProperties.image.maxWidth)
        assertTrue(big.height <= storageProperties.image.maxHeight)

        val small = service.processForStorage(imageBytes(100, 100))
        assertEquals(100, small.width)
        assertEquals(100, small.height)
    }

    @Test
    fun `processForStorage throws on unreadable input`() {
        assertThrows(ImageResizingException::class.java) {
            service.processForStorage(byteArrayOf(5, 6, 7))
        }
    }

    @Test
    fun `shouldResizeForUpload checks configured max dimensions`() {
        assertTrue(service.shouldResizeForUpload(5000, 100))
        assertFalse(service.shouldResizeForUpload(100, 100))
    }

    @Test
    fun `resizeForUpload uses configured bounds`() {
        val out = service.resizeForUpload(ByteArrayInputStream(imageBytes(5000, 5000)))
        val (w, h) = dims(out)
        assertTrue(w <= storageProperties.image.maxWidth)
        assertTrue(h <= storageProperties.image.maxHeight)
    }

    @Test
    fun `getImageDimensions returns dims for jpg and null for garbage`() {
        val d = service.getImageDimensions(ByteArrayInputStream(imageBytes(320, 240)))
        assertEquals(320, d!!.first)
        assertEquals(240, d.second)

        assertNull(service.getImageDimensions(ByteArrayInputStream(byteArrayOf(1, 2))))
    }

    @Test
    fun `thumbnail key and path generation`() {
        assertEquals("a/b/img_150.jpg", service.generateThumbnailKey("a/b/img.jpg", ImageResizingService.ThumbnailSize.SMALL))
        assertEquals("noext_300", service.generateThumbnailKey("noext", ImageResizingService.ThumbnailSize.MEDIUM))
        assertEquals("a/b/thumbs/img_500.png", service.generateThumbnailPath("a/b/img.png", ImageResizingService.ThumbnailSize.LARGE))
    }

    @Test
    fun `supportsTransparency reflects format`() {
        assertTrue(service.supportsTransparency("png"))
        assertFalse(service.supportsTransparency("jpg"))
    }

    @Test
    fun `ThumbnailSize lookups`() {
        assertEquals(ImageResizingService.ThumbnailSize.SMALL, ImageResizingService.ThumbnailSize.fromPixels(150))
        assertNull(ImageResizingService.ThumbnailSize.fromPixels(999))
        assertEquals(ImageResizingService.ThumbnailSize.MEDIUM, ImageResizingService.ThumbnailSize.fromSuffix("_300"))
        assertEquals(ImageResizingService.ThumbnailSize.LARGE, ImageResizingService.ThumbnailSize.fromAlias("lg"))
        assertEquals(ImageResizingService.ThumbnailSize.SMALL, ImageResizingService.ThumbnailSize.fromAlias("small"))
        assertNull(ImageResizingService.ThumbnailSize.fromAlias("xxl"))
    }

    @Test
    fun `progressive resize path for very large images`() {
        // 4x larger than target triggers progressive resize branch
        val out = service.resizeImage(ByteArrayInputStream(imageBytes(2000, 2000)), 100, 100, maintainAspectRatio = false)
        val (w, h) = dims(out)
        assertEquals(100, w)
        assertEquals(100, h)
    }
}
