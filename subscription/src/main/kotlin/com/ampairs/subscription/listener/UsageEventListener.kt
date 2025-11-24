package com.ampairs.subscription.listener

import com.ampairs.event.domain.events.*
import com.ampairs.subscription.domain.service.UsageTrackingService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * Listens to entity events from other modules and updates usage metrics.
 * This enables real-time usage tracking for subscription limit enforcement.
 */
@Component
class UsageEventListener(
    private val usageTrackingService: UsageTrackingService
) {
    private val logger = LoggerFactory.getLogger(UsageEventListener::class.java)

    // =====================
    // Member Events
    // =====================

    @Async
    @EventListener
    fun onMemberAdded(event: MemberAddedEvent) {
        logger.debug("Member added in workspace {}: {}", event.workspaceId, event.memberUserId)
        usageTrackingService.incrementCount(event.workspaceId, ResourceType.MEMBER)
    }

    @Async
    @EventListener
    fun onMemberRemoved(event: MemberRemovedEvent) {
        logger.debug("Member removed in workspace {}: {}", event.workspaceId, event.memberUserId)
        usageTrackingService.decrementCount(event.workspaceId, ResourceType.MEMBER)
    }

    @Async
    @EventListener
    fun onMemberActivated(event: MemberActivatedEvent) {
        logger.debug("Member activated in workspace {}: {}", event.workspaceId, event.memberUserId)
        usageTrackingService.incrementCount(event.workspaceId, ResourceType.MEMBER)
    }

    @Async
    @EventListener
    fun onMemberDeactivated(event: MemberDeactivatedEvent) {
        logger.debug("Member deactivated in workspace {}: {}", event.workspaceId, event.memberUserId)
        usageTrackingService.decrementCount(event.workspaceId, ResourceType.MEMBER)
    }

    // =====================
    // Customer Events
    // =====================

    @Async
    @EventListener
    fun onCustomerCreated(event: CustomerCreatedEvent) {
        logger.debug("Customer created in workspace {}: {}", event.workspaceId, event.entityId)
        usageTrackingService.incrementCount(event.workspaceId, ResourceType.CUSTOMER)
    }

    @Async
    @EventListener
    fun onCustomerDeleted(event: CustomerDeletedEvent) {
        logger.debug("Customer deleted in workspace {}: {}", event.workspaceId, event.entityId)
        usageTrackingService.decrementCount(event.workspaceId, ResourceType.CUSTOMER)
    }

    // =====================
    // Product Events
    // =====================

    @Async
    @EventListener
    fun onProductCreated(event: ProductCreatedEvent) {
        logger.debug("Product created in workspace {}: {}", event.workspaceId, event.entityId)
        usageTrackingService.incrementCount(event.workspaceId, ResourceType.PRODUCT)
    }

    @Async
    @EventListener
    fun onProductDeleted(event: ProductDeletedEvent) {
        logger.debug("Product deleted in workspace {}: {}", event.workspaceId, event.entityId)
        usageTrackingService.decrementCount(event.workspaceId, ResourceType.PRODUCT)
    }

    // =====================
    // Invoice Events
    // =====================

    @Async
    @EventListener
    fun onInvoiceCreated(event: InvoiceCreatedEvent) {
        logger.debug("Invoice created in workspace {}: {}", event.workspaceId, event.entityId)
        usageTrackingService.incrementCount(event.workspaceId, ResourceType.INVOICE)
    }

    @Async
    @EventListener
    fun onInvoiceDeleted(event: InvoiceDeletedEvent) {
        logger.debug("Invoice deleted in workspace {}: {}", event.workspaceId, event.entityId)
        usageTrackingService.decrementCount(event.workspaceId, ResourceType.INVOICE)
    }

    // =====================
    // Order Events
    // =====================

    @Async
    @EventListener
    fun onOrderCreated(event: OrderCreatedEvent) {
        logger.debug("Order created in workspace {}: {}", event.workspaceId, event.entityId)
        usageTrackingService.incrementCount(event.workspaceId, ResourceType.ORDER)
    }

    @Async
    @EventListener
    fun onOrderDeleted(event: OrderDeletedEvent) {
        logger.debug("Order deleted in workspace {}: {}", event.workspaceId, event.entityId)
        usageTrackingService.decrementCount(event.workspaceId, ResourceType.ORDER)
    }
}

/**
 * Resource types for usage tracking
 */
enum class ResourceType {
    CUSTOMER,
    PRODUCT,
    INVOICE,
    ORDER,
    MEMBER,
    DEVICE
}
