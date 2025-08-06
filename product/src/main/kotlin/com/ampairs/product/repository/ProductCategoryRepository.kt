package com.ampairs.product.repository

import com.ampairs.product.domain.model.group.ProductCategory
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface ProductCategoryRepository : CrudRepository<ProductCategory, Long> {
    fun findByUid(uid: String?): ProductCategory?
    fun findByRefId(refId: String?): ProductCategory?

    @Query("SELECT pc FROM product_category pc WHERE pc.uid IN (:ids)")
    fun findBySeqIds(ids: List<String>): List<ProductCategory>
}