package com.ampairs.product.domain.dto

data class ProductsCategoryResponse(
    var products: List<ProductResponse>, var categories: List<ProductCategoryResponse>
)