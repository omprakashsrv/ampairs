package com.ampairs.core.domain.dto

import com.ampairs.core.domain.model.User
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

class AuthInitRequest {
    @NotNull
    var countryCode = 91

    @NotNull
    @NotEmpty
    var phone: String? = null

    fun toUser(): User {
        val user = User()
        user.countryCode = this.countryCode
        user.phone = this.phone!!
        user.userName = this.countryCode.toString() + this.phone
        return user
    }
}
