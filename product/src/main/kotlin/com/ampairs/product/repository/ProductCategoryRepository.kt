package com.ampairs.product.repository

import com.ampairs.product.domain.model.ProductCategory
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface ProductCategoryRepository : CrudRepository<ProductCategory, String> {
    fun findByRefId(refId: String?): ProductCategory?

    @Query("SELECT pc FROM product_category pc WHERE pc.id IN (:ids)")
    fun findByIds(ids: List<String>): List<ProductCategory>
}