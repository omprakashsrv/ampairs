package com.ampairs.ecom.domain.dto

import com.ampairs.ecom.domain.enums.StockStatus
import com.ampairs.ecom.domain.model.EcomListedProduct
import java.math.BigDecimal

data class ListedProductResponse(
    val uid: String,
    val name: String,
    val brand: String?,
    val category: String?,
    val subcategory: String?,
    val unit: String?,
    val price: BigDecimal,
    val mrp: BigDecimal?,
    val stockStatus: StockStatus,
    val stockQuantity: Int,
    val imageUrls: List<String>,
    val description: String?,
)

fun EcomListedProduct.asListedProductResponse() = ListedProductResponse(
    uid = uid,
    name = name,
    brand = brand,
    category = category,
    subcategory = subcategory,
    unit = unit,
    price = price,
    mrp = mrp,
    stockStatus = stockStatus,
    stockQuantity = stockQuantity,
    imageUrls = imageUrls,
    description = description,
)
