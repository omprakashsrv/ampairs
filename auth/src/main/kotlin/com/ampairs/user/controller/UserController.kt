package com.ampairs.user.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.user.model.User
import com.ampairs.user.model.dto.UserResponse
import com.ampairs.user.model.dto.UserUpdateRequest
import com.ampairs.user.model.dto.toUserResponse
import com.ampairs.user.service.ProfilePictureNotFoundException
import com.ampairs.user.service.ProfilePictureService
import com.ampairs.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/user/v1")
class UserController(
    private val userService: UserService,
    private val profilePictureService: ProfilePictureService,
) {

    @PostMapping("/update")
    fun updateUser(@RequestBody @Valid userUpdateRequest: UserUpdateRequest): ApiResponse<UserResponse> {
        val user: User = userService.updateUser(userUpdateRequest)
        return ApiResponse.success(user.toUserResponse())
    }

    @GetMapping("")
    fun getUser(): ApiResponse<UserResponse> {
        val sessionUser = userService.getSessionUser()
        return ApiResponse.success(sessionUser.toUserResponse())
    }

    /**
     * Upload a profile picture for the current user.
     *
     * Accepts JPEG, PNG, or WebP images up to 5MB.
     * The image will be resized to a maximum of 512x512 pixels.
     * A thumbnail of 256x256 pixels will also be generated.
     *
     * @param file The image file to upload
     * @return Updated user with profile picture URLs
     */
    @PostMapping("/upload-picture", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadProfilePicture(
        @RequestPart("file") file: MultipartFile
    ): ApiResponse<UserResponse> {
        val sessionUser = userService.getSessionUser()
        val updatedUser = profilePictureService.uploadProfilePicture(sessionUser, file)
        return ApiResponse.success(updatedUser.toUserResponse())
    }

    /**
     * Delete the current user's profile picture.
     *
     * @return Updated user with null profile picture URLs
     */
    @DeleteMapping("/picture")
    fun deleteProfilePicture(): ApiResponse<UserResponse> {
        val sessionUser = userService.getSessionUser()
        val updatedUser = profilePictureService.deleteProfilePicture(sessionUser)
        return ApiResponse.success(updatedUser.toUserResponse())
    }

    /**
     * Get the current user's profile picture (full size).
     *
     * @return The profile picture image bytes
     */
    @GetMapping("/picture")
    fun getProfilePicture(): ResponseEntity<ByteArray> {
        val sessionUser = userService.getSessionUser()
        val objectKey = sessionUser.profilePictureUrl
            ?: throw ProfilePictureNotFoundException("No profile picture set")

        val imageBytes = profilePictureService.getProfilePicture(objectKey)

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
            .contentType(getMediaTypeFromKey(objectKey))
            .body(imageBytes)
    }

    /**
     * Get the current user's profile picture thumbnail.
     *
     * @return The profile picture thumbnail image bytes
     */
    @GetMapping("/picture/thumbnail")
    fun getProfilePictureThumbnail(): ResponseEntity<ByteArray> {
        val sessionUser = userService.getSessionUser()
        val objectKey = sessionUser.profilePictureThumbnailUrl
            ?: throw ProfilePictureNotFoundException("No profile picture set")

        val imageBytes = profilePictureService.getProfilePicture(objectKey)

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
            .contentType(getMediaTypeFromKey(objectKey))
            .body(imageBytes)
    }

    /**
     * Get any user's profile picture by user ID.
     *
     * @param userId The user ID
     * @return The profile picture image bytes
     */
    @GetMapping("/{userId}/picture")
    fun getUserProfilePicture(@PathVariable userId: String): ResponseEntity<ByteArray> {
        val user = userService.getUser(userId)
        val objectKey = user.profilePictureUrl
            ?: throw ProfilePictureNotFoundException("No profile picture set")

        val imageBytes = profilePictureService.getProfilePicture(objectKey)

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
            .contentType(getMediaTypeFromKey(objectKey))
            .body(imageBytes)
    }

    /**
     * Get any user's profile picture thumbnail by user ID.
     *
     * @param userId The user ID
     * @return The profile picture thumbnail image bytes
     */
    @GetMapping("/{userId}/picture/thumbnail")
    fun getUserProfilePictureThumbnail(@PathVariable userId: String): ResponseEntity<ByteArray> {
        val user = userService.getUser(userId)
        val objectKey = user.profilePictureThumbnailUrl
            ?: throw ProfilePictureNotFoundException("No profile picture set")

        val imageBytes = profilePictureService.getProfilePicture(objectKey)

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
            .contentType(getMediaTypeFromKey(objectKey))
            .body(imageBytes)
    }

    private fun getMediaTypeFromKey(objectKey: String): MediaType {
        return when {
            objectKey.endsWith(".png") -> MediaType.IMAGE_PNG
            objectKey.endsWith(".webp") -> MediaType.parseMediaType("image/webp")
            else -> MediaType.IMAGE_JPEG
        }
    }
}