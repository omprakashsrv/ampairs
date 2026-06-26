package com.ampairs.pricing.repository

import com.ampairs.pricing.domain.model.Offer
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface OfferRepository : CrudRepository<Offer, Long> {

    fun findByUid(uid: String?): Offer?

    // ── sync feed (includes inactive rows) ────────────────────────────────────────────
    @Query("SELECT o FROM offer o WHERE o.updatedAt > :updatedAt ORDER BY o.updatedAt ASC")
    fun findByUpdatedAtAfter(@Param("updatedAt") updatedAt: Instant, pageable: Pageable): Page<Offer>

    @Query("SELECT o FROM offer o")
    fun findAllForSync(pageable: Pageable): Page<Offer>
}
