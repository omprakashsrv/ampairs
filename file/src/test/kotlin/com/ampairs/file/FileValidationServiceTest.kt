package com.ampairs.file

import com.ampairs.core.service.ValidationService
import com.ampairs.file.service.FileValidationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class FileValidationServiceTest {

    private lateinit var service: FileValidationService

    @BeforeEach
    fun setUp() {
        // ValidationService has no dependencies — use the real implementation.
        service = FileValidationService(ValidationService())
    }

    private fun pngBytes(width: Int = 10, height: Int = 10): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "png", out)
        return out.toByteArray()
    }

    @Test
    fun `valid png passes and yields checksum and sanitized name`() {
        val file = MockMultipartFile("file", "photo.png", "image/png", pngBytes())

        val result = service.validateFile(file, allowedTypes = setOf("png"))

        assertTrue(result.isValid)
        assertEquals("photo.png", result.sanitizedFilename)
        assertNotNull(result.checksum)
        assertEquals(64, result.checksum!!.length) // SHA-256 hex
    }

    @Test
    fun `empty file is rejected`() {
        val file = MockMultipartFile("file", "x.png", "image/png", ByteArray(0))
        val result = service.validateFile(file)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("empty", ignoreCase = true) })
    }

    @Test
    fun `missing filename is rejected`() {
        val file = MockMultipartFile("file", "", "image/png", pngBytes())
        val result = service.validateFile(file)
        assertFalse(result.isValid)
    }

    @Test
    fun `disallowed extension is rejected`() {
        val file = MockMultipartFile("file", "photo.png", "image/png", pngBytes())
        val result = service.validateFile(file, allowedTypes = setOf("jpg"))
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("extension", ignoreCase = true) })
    }

    @Test
    fun `signature mismatch is rejected`() {
        // declared png, but content is plain bytes
        val file = MockMultipartFile("file", "photo.png", "image/png", ByteArray(32) { 7 })
        val result = service.validateFile(file, allowedTypes = setOf("png"))
        assertFalse(result.isValid)
    }

    @Test
    fun `dangerous executable signature is rejected`() {
        // PE header MZ — declared as text so it passes type checks but trips dangerous-signature scan
        val bytes = byteArrayOf(0x4D, 0x5A) + ByteArray(20)
        val file = MockMultipartFile("file", "evil.txt", "text/plain", bytes)
        val result = service.validateFile(file, allowedTypes = setOf("txt"))
        assertFalse(result.isValid)
    }

    @Test
    fun `malicious script content in text file is flagged`() {
        val bytes = "<script>alert(1)</script>".toByteArray()
        val file = MockMultipartFile("file", "note.txt", "text/plain", bytes)
        val result = service.validateFile(file, allowedTypes = setOf("txt"))
        assertFalse(result.isValid)
    }

    @Test
    fun `script content allowed when text scan disabled`() {
        val bytes = "<script>alert(1)</script>".toByteArray()
        val file = MockMultipartFile("file", "tpl.html", "text/html", bytes)
        val result = service.validateFile(file, allowedTypes = setOf("html"), scanMaliciousText = false)
        assertTrue(result.isValid)
    }

    @Test
    fun `validateFiles aggregates batch results`() {
        val good = MockMultipartFile("file", "a.png", "image/png", pngBytes())
        val bad = MockMultipartFile("file", "b.png", "image/png", ByteArray(0))

        val batch = service.validateFiles(listOf(good, bad), allowedTypes = setOf("png"))

        assertFalse(batch.allValid)
        assertEquals(2, batch.results.size)
        assertTrue(batch.results[0].isValid)
        assertFalse(batch.results[1].isValid)
    }
}
