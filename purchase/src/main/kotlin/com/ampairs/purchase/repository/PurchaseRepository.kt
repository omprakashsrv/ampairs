package com.ampairs.purchase.repository

import com.ampairs.purchase.domain.enums.PurchaseStatus
import com.ampairs.purchase.domain.model.Purchase
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface PurchaseRepository : CrudRepository<Purchase, Long>, PagingAndSortingRepository<Purchase, Long> {
    @EntityGraph("Purchase.withItems")
    fun findByUid(uid: String): Optional<Purchase>

    fun findByPurchaseNumber(purchaseNumber: String): Optional<Purchase>

    @EntityGraph("Purchase.withItems")
    fun findBySupplierId(supplierId: String): List<Purchase>

    @EntityGraph("Purchase.withItems")
    fun findByStatus(status: PurchaseStatus): List<Purchase>

    @Query("SELECT MAX(CAST(p.purchaseNumber AS INTEGER)) FROM purchase p")
    fun findMaxPurchaseNumber(): Optional<String>

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(p.updatedAt) FROM purchase p")
    fun findMaxUpdatedAt(): Instant?
}
