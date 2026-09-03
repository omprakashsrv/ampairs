package com.ampairs.cb_maintenance.service

import com.ampairs.cb_maintenance.domain.dto.TicketBucketRequest
import com.ampairs.cb_maintenance.domain.dto.TicketBucketResponse
import com.ampairs.cb_maintenance.domain.dto.applyRequest
import com.ampairs.cb_maintenance.domain.dto.asTicketBucketResponse
import com.ampairs.cb_maintenance.domain.model.TicketBucket
import com.ampairs.cb_maintenance.repository.TicketBucketRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Read side of the global ticket-classification catalog. The taxonomy is seeded by Flyway and is
 * the same for every workspace, so the feed is pull-only; [bulkUpsert] exists only so the canonical
 * `/sync` POST contract is honored (admin corrections), not for offline device writes.
 */
@Service
class TicketBucketService(
    private val repository: TicketBucketRepository,
) {
    private val logger = LoggerFactory.getLogger(TicketBucketService::class.java)

    @Transactional(readOnly = true)
    fun getAfterSync(lastSync: String?, pageable: Pageable): Page<TicketBucketResponse> {
        val page: Page<TicketBucket> = if (lastSync.isNullOrBlank()) {
            repository.findAllForSync(pageable)
        } else {
            try {
                repository.findByUpdatedAtAfter(
                    Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8)), pageable,
                )
            } catch (e: Exception) {
                logger.warn("Invalid last_sync '{}', full feed", lastSync, e)
                repository.findAllForSync(pageable)
            }
        }
        return page.map { it.asTicketBucketResponse() }
    }

    @Transactional
    fun bulkUpsert(requests: List<TicketBucketRequest>): List<TicketBucketResponse> =
        requests.map { request ->
            val existing = request.uid?.takeIf { it.isNotBlank() }?.let { repository.findByUid(it) }
            val entity = (existing ?: TicketBucket()).applyRequest(request)
            repository.save(entity).asTicketBucketResponse()
        }
}
