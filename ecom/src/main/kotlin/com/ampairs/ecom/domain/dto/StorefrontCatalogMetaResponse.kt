package com.ampairs.ecom.domain.dto

data class SubcategoryMeta(
    val name: String,
    val productCount: Int,
)

data class CategoryMeta(
    val name: String,
    val productCount: Int,
    val subcategories: List<SubcategoryMeta>,
)

data class BrandMeta(
    val name: String,
    val productCount: Int,
)

data class StorefrontCatalogMetaResponse(
    val categories: List<CategoryMeta>,
    val brands: List<BrandMeta>,
)
