package com.ampairs.product.repository

import com.ampairs.product.domain.model.ProductSubCategory
import org.springframework.data.repository.CrudRepository

interface ProductSubCategoryRepository : CrudRepository<ProductSubCategory, String> {
    fun findByRefId(refId: String?): ProductSubCategory?
}