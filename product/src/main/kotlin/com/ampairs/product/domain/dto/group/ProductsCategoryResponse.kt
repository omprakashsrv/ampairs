package com.ampairs.product.domain.dto.group

import com.ampairs.product.domain.dto.product.ProductResponse

data class ProductsCategoryResponse(
    var products: List<ProductResponse>, var categories: List<ProductCategoryResponse>
)