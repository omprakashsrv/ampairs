package com.ampairs.pricing.repository

import com.ampairs.pricing.domain.model.PriceListItem
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface PriceListItemRepository : CrudRepository<PriceListItem, Long> {

    fun findByUid(uid: String?): PriceListItem?

    fun findByPriceListId(priceListId: String): List<PriceListItem>

    /** Candidate items for a product within a list — caller prefers a variant match over base. */
    fun findByPriceListIdAndProductIdAndActiveTrue(priceListId: String, productId: String): List<PriceListItem>

    // ── sync feed (includes inactive rows) ────────────────────────────────────────────
    @Query("SELECT i FROM price_list_item i WHERE i.updatedAt > :updatedAt ORDER BY i.updatedAt ASC")
    fun findByUpdatedAtAfter(@Param("updatedAt") updatedAt: Instant, pageable: Pageable): Page<PriceListItem>

    @Query("SELECT i FROM price_list_item i")
    fun findAllForSync(pageable: Pageable): Page<PriceListItem>

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(i.updatedAt) FROM price_list_item i")
    fun findMaxUpdatedAt(): Instant?
}
