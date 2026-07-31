package com.ampairs.analytics.repository

import com.ampairs.analytics.domain.model.DemandForecast
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Persistence for [DemandForecast]. Exposes the incremental `/sync` pull feed (pull-only resource).
 * `@TenantId` scopes every query to the current workspace.
 */
@Repository
interface DemandForecastRepository : JpaRepository<DemandForecast, Long> {

    /** Incremental sync feed: forecasts updated at/after [lastUpdated], ordered by the pageable. */
    fun findByUpdatedAtGreaterThanEqual(lastUpdated: Instant, pageable: Pageable): Page<DemandForecast>

    /** Full feed (first sync, no checkpoint). */
    fun findAllBy(pageable: Pageable): Page<DemandForecast>

    fun findByProductIdAndPeriodStartAndHorizon(
        productId: String,
        periodStart: java.time.LocalDate,
        horizon: Int,
    ): DemandForecast?

    /** Most recent forecast for a product (drives the demand signal). @TenantId-scoped. */
    fun findFirstByProductIdOrderByPeriodStartDesc(productId: String): DemandForecast?
}
