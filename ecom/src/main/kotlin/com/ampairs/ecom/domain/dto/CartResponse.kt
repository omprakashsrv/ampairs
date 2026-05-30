package com.ampairs.ecom.domain.dto

import com.ampairs.ecom.domain.enums.CartStatus
import com.ampairs.ecom.domain.model.EcomCart
import com.ampairs.ecom.domain.model.EcomCartItem
import java.math.BigDecimal
import java.time.Instant

data class CartItemResponse(
    val uid: String,
    val listedProductId: String,
    val managementProductId: String,
    val productName: String,
    val unitPrice: BigDecimal,
    val quantity: Int,
    val primaryImageUrl: String?,
    val lineTotal: BigDecimal,
)

data class CartResponse(
    val uid: String,
    val sessionToken: String,
    val status: CartStatus,
    val expiresAt: Instant,
    val items: List<CartItemResponse>,
    val subtotal: BigDecimal,
)

fun EcomCartItem.asCartItemResponse() = CartItemResponse(
    uid = uid,
    listedProductId = listedProductId,
    managementProductId = managementProductId,
    productName = productName,
    unitPrice = unitPrice,
    quantity = quantity,
    primaryImageUrl = primaryImageUrl,
    lineTotal = unitPrice.multiply(BigDecimal(quantity)),
)

fun EcomCart.asCartResponse() = CartResponse(
    uid = uid,
    sessionToken = sessionToken,
    status = status,
    expiresAt = expiresAt,
    items = cartItems.map { it.asCartItemResponse() },
    subtotal = cartItems.fold(BigDecimal.ZERO) { acc, item -> acc + item.unitPrice.multiply(BigDecimal(item.quantity)) },
)
