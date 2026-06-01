package com.ampairs.product.repository

import com.ampairs.product.domain.model.group.ProductSubCategory
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.repository.CrudRepository

interface ProductSubCategoryRepository : CrudRepository<ProductSubCategory, Long> {
    fun findByUid(uid: String?): ProductSubCategory?
    fun findByRefId(refId: String?): ProductSubCategory?

    @EntityGraph("ProductSubCategory.withImage")
    override fun findAll(): List<ProductSubCategory>
}