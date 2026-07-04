package com.ampairs.pricing.repository

import com.ampairs.core.domain.enums.SalesChannel
import com.ampairs.pricing.domain.enums.OfferStatus
import com.ampairs.pricing.domain.model.Offer
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface OfferRepository : CrudRepository<Offer, Long> {

    fun findByUid(uid: String?): Offer?

    /** Active, ACTIVE-status offers for a channel — the application candidate set (@TenantId-filtered). */
    fun findByChannelAndStatusAndActiveTrue(channel: SalesChannel, status: OfferStatus): List<Offer>

    /** Active coupon offers for a channel whose code matches (case-insensitive) — coupon lookup. */
    fun findByChannelAndStatusAndActiveTrueAndCouponCodeIgnoreCase(
        channel: SalesChannel,
        status: OfferStatus,
        couponCode: String,
    ): List<Offer>

    // ── sync feed (includes inactive rows) ────────────────────────────────────────────
    @Query("SELECT o FROM offer o WHERE o.updatedAt > :updatedAt ORDER BY o.updatedAt ASC")
    fun findByUpdatedAtAfter(@Param("updatedAt") updatedAt: Instant, pageable: Pageable): Page<Offer>

    @Query("SELECT o FROM offer o")
    fun findAllForSync(pageable: Pageable): Page<Offer>

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(o.updatedAt) FROM offer o")
    fun findMaxUpdatedAt(): Instant?
}
