package com.ampairs.ecom.domain.dto

import com.ampairs.ecom.domain.enums.TaxonomyType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class TaxonomyImageRequest(
    @field:NotNull val type: TaxonomyType,
    @field:NotBlank val name: String,
    @field:NotBlank val imageUrl: String,
    val sortOrder: Int = 0,
)

data class TaxonomyImageResponse(
    val uid: String,
    val type: TaxonomyType,
    val name: String,
    val imageUrl: String,
    val sortOrder: Int,
)
