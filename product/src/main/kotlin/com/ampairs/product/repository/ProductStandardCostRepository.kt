package com.ampairs.product.repository

import com.ampairs.product.domain.model.ProductStandardCost
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ProductStandardCostRepository : CrudRepository<ProductStandardCost, Long> {

    fun findByUid(uid: String?): ProductStandardCost?

    /** All active cost versions for a product — caller picks the one effective at the voucher date. */
    fun findByProductIdAndActiveTrue(productId: String): List<ProductStandardCost>

    // ── sync feed (includes inactive rows so deletions propagate) ──────────────────────
    @Query("SELECT c FROM product_standard_cost c WHERE c.updatedAt > :updatedAt ORDER BY c.updatedAt ASC")
    fun findByUpdatedAtAfter(@Param("updatedAt") updatedAt: Instant, pageable: Pageable): Page<ProductStandardCost>

    @Query("SELECT c FROM product_standard_cost c")
    fun findAllForSync(pageable: Pageable): Page<ProductStandardCost>
}
