package com.ampairs.product.repository

import com.ampairs.product.domain.model.group.ProductSubCategory
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.time.Instant

interface ProductSubCategoryRepository : CrudRepository<ProductSubCategory, Long> {
    fun findByUid(uid: String?): ProductSubCategory?
    fun findByRefId(refId: String?): ProductSubCategory?

    @EntityGraph("ProductSubCategory.withImage")
    override fun findAll(): List<ProductSubCategory>

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(psc.updatedAt) FROM product_sub_category psc")
    fun findMaxUpdatedAt(): Instant?
}