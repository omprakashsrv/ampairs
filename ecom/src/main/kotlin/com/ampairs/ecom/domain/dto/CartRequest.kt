package com.ampairs.ecom.domain.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class CartItemRequest(
    @field:NotNull
    val listedProductId: String,

    @field:NotNull
    @field:Min(1)
    val quantity: Int,
)
