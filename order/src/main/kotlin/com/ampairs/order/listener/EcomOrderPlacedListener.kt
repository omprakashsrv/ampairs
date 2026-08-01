package com.ampairs.order.listener

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.event.domain.kafka.EcomOrderPlacedEvent
import com.ampairs.order.service.EcomOrderIngestionService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Creates a management order when a storefront order is placed. In-process + asynchronous: the ecom
 * module publishes an [EcomOrderPlacedEvent] via Spring events; this listener runs after the
 * checkout transaction commits, on a separate thread — no Kafka required.
 *
 * As the async entry point (like a message consumer) it owns the tenant context.
 */
@Component
class EcomOrderPlacedListener(
    private val ingestionService: EcomOrderIngestionService,
) {
    private val log = LoggerFactory.getLogger(EcomOrderPlacedListener::class.java)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onEcomOrderPlaced(event: EcomOrderPlacedEvent) {
        TenantContextHolder.setCurrentTenant(event.workspaceId)
        try {
            ingestionService.ingest(event)
        } catch (e: Exception) {
            log.error("Failed to ingest ecom order {}: {}", event.ecomOrderRef, e.message, e)
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }
}
