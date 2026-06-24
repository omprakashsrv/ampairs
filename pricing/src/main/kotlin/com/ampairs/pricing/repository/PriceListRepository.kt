package com.ampairs.pricing.repository

import com.ampairs.core.domain.enums.SalesChannel
import com.ampairs.pricing.domain.enums.PriceListStatus
import com.ampairs.pricing.domain.model.PriceList
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface PriceListRepository : CrudRepository<PriceList, Long> {

    fun findByUid(uid: String?): PriceList?

    /** Active ACTIVE-status lists for a channel — resolution candidate set (@TenantId-filtered). */
    fun findByChannelAndStatusAndActiveTrue(channel: SalesChannel, status: PriceListStatus): List<PriceList>

    // ── sync feed (includes inactive rows) ────────────────────────────────────────────
    @Query("SELECT p FROM price_list p WHERE p.updatedAt > :updatedAt ORDER BY p.updatedAt ASC")
    fun findByUpdatedAtAfter(@Param("updatedAt") updatedAt: Instant, pageable: Pageable): Page<PriceList>

    @Query("SELECT p FROM price_list p")
    fun findAllForSync(pageable: Pageable): Page<PriceList>
}
