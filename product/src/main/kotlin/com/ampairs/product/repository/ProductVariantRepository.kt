package com.ampairs.product.repository

import com.ampairs.product.domain.model.ProductVariant
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant

@Repository
interface ProductVariantRepository : CrudRepository<ProductVariant, Long> {

    fun findByUid(uid: String): ProductVariant?

    fun findBySku(sku: String): ProductVariant?

    @Query("SELECT v FROM ProductVariant v WHERE v.productId = :productId AND v.active = true ORDER BY v.variantName")
    fun findActiveVariantsByProductId(@Param("productId") productId: String): List<ProductVariant>

    @Query("SELECT v FROM ProductVariant v WHERE v.productId = :productId ORDER BY v.variantName")
    fun findAllVariantsByProductId(@Param("productId") productId: String): List<ProductVariant>

    @Query("SELECT SUM(v.stockQuantity) FROM ProductVariant v WHERE v.productId = :productId AND v.active = true")
    fun getTotalStockByProductId(@Param("productId") productId: String): BigDecimal?

    @Query("SELECT v FROM ProductVariant v WHERE v.updatedAt >= :timestamp")
    fun findVariantsUpdatedAfter(@Param("timestamp") timestamp: Instant): List<ProductVariant>

    @Query("SELECT v FROM ProductVariant v WHERE v.synced = false")
    fun findUnsyncedVariants(): List<ProductVariant>

    fun deleteByUid(uid: String)

    // Batch query methods for performance
    fun findAllByProductIdIn(productIds: List<String>): List<ProductVariant>
}
