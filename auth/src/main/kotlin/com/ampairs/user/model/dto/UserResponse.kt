package com.ampairs.user.model.dto

import com.ampairs.user.model.User

data class UserResponse(
    var id: String,
    var firstName: String,
    var lastName: String?,
    var userName: String?,
    var countryCode: Int,
    var phone: String,
)

fun User.toUserResponse(): UserResponse {
    return UserResponse(
        id = this.seqId,
        firstName = this.firstName,
        lastName = this.lastName,
        countryCode = this.countryCode,
        phone = this.phone,
        userName = this.userName,
    )
}