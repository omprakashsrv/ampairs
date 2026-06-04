package com.ampairs.product.repository

import com.ampairs.product.domain.model.group.ProductBrand
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.time.Instant

interface ProductBrandRepository : CrudRepository<ProductBrand, Long> {
    fun findByUid(uid: String?): ProductBrand?
    fun findByRefId(refId: String?): ProductBrand?

    @EntityGraph("ProductBrand.withImage")
    override fun findAll(): List<ProductBrand>

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(pb.updatedAt) FROM product_brand pb")
    fun findMaxUpdatedAt(): Instant?
}