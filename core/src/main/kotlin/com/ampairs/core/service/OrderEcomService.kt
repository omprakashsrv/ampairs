package com.ampairs.core.service

interface OrderEcomService {
    fun confirmEcomOrder(ecomOrderRef: String, confirmedItems: List<ConfirmedLineItem>)
}

data class ConfirmedLineItem(
    val managementProductId: String,
    val quantityConfirmed: Int,
    val status: String,
)
