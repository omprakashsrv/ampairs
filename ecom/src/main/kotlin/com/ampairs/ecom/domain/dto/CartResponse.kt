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
    val brand: String?,
    val unit: String?,
    val unitPrice: BigDecimal,
    val mrpAtAdd: BigDecimal?,
    val quantity: Int,
    val primaryImageUrl: String?,
    val lineTotal: BigDecimal,
    val lineMrp: BigDecimal?,
)

data class CartResponse(
    val uid: String,
    val sessionToken: String,
    val status: CartStatus,
    val expiresAt: Instant,
    val items: List<CartItemResponse>,
    val subtotal: BigDecimal,
    val itemTotalMrp: BigDecimal?,
    val savings: BigDecimal?,
)

fun EcomCartItem.asCartItemResponse() = CartItemResponse(
    uid = uid,
    listedProductId = listedProductId,
    managementProductId = managementProductId,
    productName = productName,
    brand = brand,
    unit = unit,
    unitPrice = unitPrice,
    mrpAtAdd = mrpAtAdd,
    quantity = quantity,
    primaryImageUrl = primaryImageUrl,
    lineTotal = unitPrice.multiply(BigDecimal(quantity)),
    lineMrp = mrpAtAdd?.multiply(BigDecimal(quantity)),
)

fun EcomCart.asCartResponse(): CartResponse {
    val items = cartItems.map { it.asCartItemResponse() }
    val subtotal = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.lineTotal }
    val hasMrp = items.any { it.lineMrp != null }
    val itemTotalMrp = if (hasMrp) items.fold(BigDecimal.ZERO) { acc, item -> acc + (item.lineMrp ?: item.lineTotal) } else null
    val savings = itemTotalMrp?.subtract(subtotal)?.takeIf { it > BigDecimal.ZERO }
    return CartResponse(
        uid = uid,
        sessionToken = sessionToken,
        status = status,
        expiresAt = expiresAt,
        items = items,
        subtotal = subtotal,
        itemTotalMrp = itemTotalMrp,
        savings = savings,
    )
}
