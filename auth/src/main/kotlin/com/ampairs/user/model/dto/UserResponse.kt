package com.ampairs.user.model.dto

import com.ampairs.user.model.User

data class UserResponse(
    val id: String,
    val firstName: String,
    val lastName: String?,
    val userName: String,
    val countryCode: Int,
    val phone: String,
    val email: String? = null,
    val fullName: String,
    val active: Boolean,
    val profilePictureUrl: String? = null,
    val profilePictureThumbnailUrl: String? = null,
)

fun User.toUserResponse(): UserResponse {
    return UserResponse(
        id = this.uid,
        firstName = this.firstName,
        lastName = this.lastName,
        userName = this.userName,
        countryCode = this.countryCode,
        phone = this.phone,
        email = this.email,
        fullName = this.getFullName(),
        active = this.active,
        profilePictureUrl = this.profilePictureUrl,
        profilePictureThumbnailUrl = this.profilePictureThumbnailUrl,
    )
}