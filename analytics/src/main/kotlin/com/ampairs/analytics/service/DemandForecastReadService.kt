package com.ampairs.analytics.service

import com.ampairs.analytics.domain.dto.DemandForecastResponse
import com.ampairs.analytics.domain.dto.asResponse
import com.ampairs.analytics.repository.DemandForecastRepository
import com.ampairs.core.domain.dto.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Serves the pull-only `DemandForecast` `/sync` feed. Incremental by `updatedAt` (ASC) so the client
 * checkpoint advances monotonically; no push half (forecasts are server-generated).
 */
@Service
@Transactional(readOnly = true)
class DemandForecastReadService(
    private val forecastRepository: DemandForecastRepository,
) {

    fun syncFeed(lastSync: Instant?, page: Int, size: Int): PageResponse<DemandForecastResponse> {
        val pageable = PageRequest.of(page, size.coerceAtMost(100), Sort.by(Sort.Direction.ASC, "updatedAt"))
        val result = if (lastSync == null) {
            forecastRepository.findAllBy(pageable)
        } else {
            forecastRepository.findByUpdatedAtGreaterThanEqual(lastSync, pageable)
        }
        return PageResponse.from(result) { it.asResponse() }
    }
}
