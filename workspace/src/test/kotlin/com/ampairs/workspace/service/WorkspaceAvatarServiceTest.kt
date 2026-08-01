package com.ampairs.workspace.service

import com.ampairs.file.config.StorageProperties
import com.ampairs.file.service.ImageResizingService
import com.ampairs.file.storage.ObjectStorageService
import com.ampairs.workspace.model.Workspace
import com.ampairs.workspace.repository.WorkspaceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.web.multipart.MultipartFile
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceAvatarServiceTest {

    @Mock
    private lateinit var objectStorageService: ObjectStorageService

    @Mock
    private lateinit var imageResizingService: ImageResizingService

    @Mock
    private lateinit var workspaceRepository: WorkspaceRepository

    private val storageProperties = StorageProperties()

    private lateinit var service: WorkspaceAvatarService

    private fun pngBytes(): ByteArray {
        val img = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
        val out = ByteArrayOutputStream()
        ImageIO.write(img, "png", out)
        return out.toByteArray()
    }

    private fun workspace() = Workspace().apply { uid = "WSP-1"; name = "Acme" }

    @BeforeEach
    fun setUp() {
        service = WorkspaceAvatarService(objectStorageService, imageResizingService, storageProperties, workspaceRepository)
        whenever(workspaceRepository.save(any<Workspace>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `uploadAvatar processes valid png and stores object keys`() {
        val bytes = pngBytes()
        val file = mock<MultipartFile>()
        whenever(file.isEmpty).thenReturn(false)
        whenever(file.size).thenReturn(bytes.size.toLong())
        whenever(file.contentType).thenReturn("image/png")
        whenever(file.bytes).thenReturn(bytes)
        whenever(imageResizingService.getImageDimensions(any())).thenReturn(Pair(10, 10))
        whenever(imageResizingService.resizeImage(any(), any(), any(), any(), any())).thenReturn(bytes)
        whenever(objectStorageService.listObjects(any(), any(), any())).thenReturn(emptyList())

        val result = service.uploadAvatar(workspace(), file)

        assertEquals(true, result.avatarUrl!!.startsWith("workspace-avatars/WSP-1/"))
        assertEquals(true, result.avatarThumbnailUrl!!.contains("avatar_thumb_"))
        // full-size + thumbnail uploads
        verify(objectStorageService, org.mockito.kotlin.times(2)).uploadFile(any<ByteArray>(), any(), any(), any(), any())
    }

    @Test
    fun `uploadAvatar rejects empty file`() {
        val file = mock<MultipartFile>()
        whenever(file.isEmpty).thenReturn(true)

        assertThrows(WorkspaceAvatarValidationException::class.java) {
            service.uploadAvatar(workspace(), file)
        }
    }

    @Test
    fun `uploadAvatar rejects oversized file`() {
        val file = mock<MultipartFile>()
        whenever(file.isEmpty).thenReturn(false)
        whenever(file.size).thenReturn(6L * 1024 * 1024)

        assertThrows(WorkspaceAvatarValidationException::class.java) {
            service.uploadAvatar(workspace(), file)
        }
    }

    @Test
    fun `uploadAvatar rejects disallowed content type`() {
        val file = mock<MultipartFile>()
        whenever(file.isEmpty).thenReturn(false)
        whenever(file.size).thenReturn(100L)
        whenever(file.contentType).thenReturn("application/pdf")

        assertThrows(WorkspaceAvatarValidationException::class.java) {
            service.uploadAvatar(workspace(), file)
        }
    }

    @Test
    fun `uploadAvatar rejects content not matching declared type`() {
        val file = mock<MultipartFile>()
        whenever(file.isEmpty).thenReturn(false)
        whenever(file.size).thenReturn(100L)
        whenever(file.contentType).thenReturn("image/png")
        // JPEG-ish bytes declared as PNG → signature mismatch
        whenever(file.bytes).thenReturn(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0, 0, 0, 0, 0))

        assertThrows(WorkspaceAvatarValidationException::class.java) {
            service.uploadAvatar(workspace(), file)
        }
    }

    @Test
    fun `getAvatar returns bytes`() {
        val bytes = pngBytes()
        whenever(objectStorageService.downloadFile(eq(storageProperties.defaultBucket), eq("key")))
            .thenReturn(ByteArrayInputStream(bytes))

        val result = service.getAvatar("key")

        assertEquals(bytes.size, result.size)
    }

    @Test
    fun `getAvatar throws not found on failure`() {
        whenever(objectStorageService.downloadFile(any(), any())).thenThrow(RuntimeException("missing"))

        assertThrows(WorkspaceAvatarNotFoundException::class.java) { service.getAvatar("key") }
    }

    @Test
    fun `deleteAvatar clears avatar fields and removes old files`() {
        whenever(objectStorageService.listObjects(any(), any(), any())).thenReturn(emptyList())
        val ws = workspace().apply { avatarUrl = "k1"; avatarThumbnailUrl = "k2" }

        val result = service.deleteAvatar(ws)

        assertNull(result.avatarUrl)
        assertNull(result.avatarThumbnailUrl)
        verify(workspaceRepository).save(ws)
    }
}
