package com.ampairs.user.model.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UserUpdateRequest(
    @field:NotBlank(message = "First name cannot be blank")
    @field:Size(max = 100, message = "First name must not exceed 100 characters")
    val firstName: String,

    @field:NotBlank(message = "Last name cannot be blank")
    @field:Size(max = 100, message = "Last name must not exceed 100 characters")
    val lastName: String,
)