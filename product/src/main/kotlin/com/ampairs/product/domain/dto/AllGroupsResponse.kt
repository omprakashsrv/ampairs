package com.ampairs.product.domain.dto

data class AllGroupsResponse(
    val groups: List<ProductGroupResponse>,
    val brands: List<ProductBrandResponse>,
    val categories: List<ProductCategoryResponse>,
    val subCategories: List<ProductSubCategoryResponse>,
)