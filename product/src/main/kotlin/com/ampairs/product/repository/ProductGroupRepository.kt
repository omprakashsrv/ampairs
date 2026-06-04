package com.ampairs.product.repository

import com.ampairs.product.domain.model.group.ProductGroup
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.time.Instant

interface ProductGroupRepository : CrudRepository<ProductGroup, Long> {
    fun findByUid(uid: String?): ProductGroup?
    fun findByRefId(refId: String?): ProductGroup?

    @EntityGraph("ProductGroup.withImage")
    override fun findAll(): List<ProductGroup>

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(pg.updatedAt) FROM product_group pg")
    fun findMaxUpdatedAt(): Instant?
}