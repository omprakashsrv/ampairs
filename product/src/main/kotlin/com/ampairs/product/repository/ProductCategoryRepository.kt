package com.ampairs.product.repository

import com.ampairs.product.domain.model.ProductCategory
import org.springframework.data.repository.CrudRepository

interface ProductCategoryRepository : CrudRepository<ProductCategory, String> {
    fun findByRefId(refId: String?): ProductCategory?
}