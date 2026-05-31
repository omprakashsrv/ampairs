package com.ampairs.ecom.domain.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class StorefrontRequest(
    @field:NotBlank
    val name: String,

    @field:NotBlank
    @field:Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, digits, and hyphens")
    @field:Size(min = 3, max = 50)
    val slug: String,

    val description: String? = null,
    val logoUrl: String? = null,
    val bannerUrl: String? = null,
)
