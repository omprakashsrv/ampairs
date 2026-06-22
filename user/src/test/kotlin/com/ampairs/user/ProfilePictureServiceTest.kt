package com.ampairs.user

import com.ampairs.file.config.StorageProperties
import com.ampairs.file.service.ImageResizingService
import com.ampairs.file.storage.ObjectStorageService
import com.ampairs.file.storage.ObjectSummary
import com.ampairs.file.storage.UploadResult
import com.ampairs.user.model.User
import com.ampairs.user.repository.UserRepository
import com.ampairs.user.service.ProfilePictureService
import com.ampairs.user.service.ProfilePictureNotFoundException
import com.ampairs.user.service.ProfilePictureValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
class ProfilePictureServiceTest {

    @Mock
    private lateinit var objectStorageService: ObjectStorageService

    @Mock
    private lateinit var imageResizingService: ImageResizingService

    @Mock
    private lateinit var userRepository: UserRepository

    private val storageProperties = StorageProperties()
    private lateinit var service: ProfilePictureService

    private fun pngBytes(): ByteArray {
        val image = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "png", out)
        return out.toByteArray()
    }

    private fun pngFile(): MultipartFile =
        MockMultipartFile("file", "avatar.png", "image/png", pngBytes())

    @BeforeEach
    fun setUp() {
        service = ProfilePictureService(objectStorageService, imageResizingService, storageProperties, userRepository)
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }
        whenever(objectStorageService.listObjects(any(), any(), any())).thenReturn(emptyList())
        whenever(objectStorageService.uploadFile(any<ByteArray>(), any(), any(), any(), any()))
            .thenReturn(UploadResult("k", "etag", 1L, null, null))
        whenever(imageResizingService.resizeImage(any(), any(), any(), any(), any()))
            .thenReturn(byteArrayOf(1, 2, 3))
    }

    @Test
    fun `uploadProfilePicture stores keys and timestamp on the user`() {
        val user = User().apply { uid = "USR-1"; userName = "alice"; firstName = "Alice" }

        val result = service.uploadProfilePicture(user, pngFile())

        assertNotNull(result.profilePictureUrlField)
        assertNotNull(result.profilePictureThumbnailUrl)
        assertNotNull(result.profilePictureUpdatedAt)
        verify(userRepository).save(user)
        // full + thumbnail uploaded
        verify(objectStorageService, org.mockito.kotlin.times(2))
            .uploadFile(any<ByteArray>(), any(), any(), any(), any())
    }

    @Test
    fun `uploadProfilePicture rejects an empty file`() {
        val empty = MockMultipartFile("file", "avatar.png", "image/png", ByteArray(0))
        val user = User().apply { uid = "USR-1" }

        assertThrows(ProfilePictureValidationException::class.java) {
            service.uploadProfilePicture(user, empty)
        }
        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `uploadProfilePicture rejects a disallowed content type`() {
        val pdf = MockMultipartFile("file", "doc.pdf", "application/pdf", pngBytes())
        val user = User().apply { uid = "USR-1" }

        assertThrows(ProfilePictureValidationException::class.java) {
            service.uploadProfilePicture(user, pdf)
        }
    }

    @Test
    fun `uploadProfilePicture rejects content whose signature does not match the declared type`() {
        // declared png but content is not a valid png signature
        val fake = MockMultipartFile("file", "avatar.png", "image/png", ByteArray(16) { 1 })
        val user = User().apply { uid = "USR-1" }

        assertThrows(ProfilePictureValidationException::class.java) {
            service.uploadProfilePicture(user, fake)
        }
    }

    @Test
    fun `getProfilePicture returns bytes from storage`() {
        whenever(objectStorageService.downloadFile(eq(storageProperties.defaultBucket), eq("k")))
            .thenReturn(ByteArrayInputStream(byteArrayOf(1, 2, 3)))

        val bytes = service.getProfilePicture("k")
        assertEquals(3, bytes.size)
    }

    @Test
    fun `getProfilePicture throws not-found when download fails`() {
        whenever(objectStorageService.downloadFile(any(), any())).thenThrow(RuntimeException("boom"))

        assertThrows(ProfilePictureNotFoundException::class.java) { service.getProfilePicture("k") }
    }

    @Test
    fun `deleteProfilePicture clears keys`() {
        val user = User().apply {
            uid = "USR-1"
            profilePictureUrlField = "old/full.png"
            profilePictureThumbnailUrl = "old/thumb.png"
        }

        val result = service.deleteProfilePicture(user)

        assertNull(result.profilePictureUrlField)
        assertNull(result.profilePictureThumbnailUrl)
        assertNotNull(result.profilePictureUpdatedAt)
        verify(userRepository).save(user)
    }
}
