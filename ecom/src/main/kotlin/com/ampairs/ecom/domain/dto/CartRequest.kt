package com.ampairs.ecom.domain.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class CartItemRequest(
    @field:NotNull
    val listedProductId: String,

    // 0 is allowed and means "remove this line from the cart".
    @field:NotNull
    @field:Min(0)
    val quantity: Int,
)
