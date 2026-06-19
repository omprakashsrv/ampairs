package com.ampairs.ecom.listener

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.ecom.service.EcomOrderService
import com.ampairs.event.domain.kafka.EcomOrderStatusEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Applies management-order status changes (published by the order module) back onto the storefront
 * order. In-process + asynchronous: the order module publishes an [EcomOrderStatusEvent] via Spring
 * events; this listener runs after that transaction commits, on a separate thread.
 *
 * As the async entry point (like a message consumer) it owns the tenant context.
 * Kafka-extensible: a future bridge can republish the same payload to a topic.
 */
@Component
class EcomOrderStatusListener(
    private val ecomOrderService: EcomOrderService,
) {
    private val log = LoggerFactory.getLogger(EcomOrderStatusListener::class.java)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onEcomOrderStatus(event: EcomOrderStatusEvent) {
        TenantContextHolder.setCurrentTenant(event.workspaceId)
        try {
            ecomOrderService.applyStatusUpdate(event)
        } catch (e: Exception) {
            log.error("Failed to apply ecom order status for {}: {}", event.ecomOrderRef, e.message, e)
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }
}
