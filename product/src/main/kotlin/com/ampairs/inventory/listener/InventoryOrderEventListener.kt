package com.ampairs.inventory.listener

import com.ampairs.event.domain.events.OrderCreatedEvent
import com.ampairs.event.domain.events.OrderStatusChangedEvent
import com.ampairs.inventory.repository.InventoryConfigRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * Inventory Order Event Listener
 *
 * Listens to order events and logs them for audit/monitoring purposes.
 *
 * IMPORTANT - Integration Architecture:
 * This listener demonstrates event-driven architecture but does NOT perform
 * automatic inventory deduction to avoid circular dependencies.
 *
 * For actual inventory integration, the Order Service should:
 * 1. Inject InventoryTransactionService directly (if in same module)
 * 2. OR call Inventory REST APIs (if separate microservices)
 * 3. Call stockOut() when order status changes to CONFIRMED/PROCESSING
 * 4. Call stockIn() to restore when order is CANCELLED/REFUNDED
 *
 * Configuration:
 * - InventoryConfig.autoDeductOnOrder: Controls whether Order Service should deduct inventory
 * - InventoryConfig.blockOrdersWhenOutOfStock: Order Service checks stock before confirmation
 *
 * Example Order Service Integration:
 * ```
 * fun confirmOrder(orderId: String) {
 *     val order = orderRepository.findByUid(orderId)
 *     val config = inventoryConfigService.getOrCreateConfig()
 *
 *     if (config.autoDeductOnOrder) {
 *         order.orderItems.forEach { item ->
 *             val request = StockOutRequest(
 *                 inventoryItemId = item.productId,
 *                 warehouseId = config.defaultWarehouseId,
 *                 quantity = item.quantity,
 *                 reason = "SALE",
 *                 referenceType = "ORDER",
 *                 referenceId = order.uid
 *             )
 *             inventoryTransactionService.stockOut(request)
 *         }
 *     }
 *
 *     order.status = OrderStatus.CONFIRMED
 *     orderRepository.save(order)
 * }
 * ```
 */
@Component
class InventoryOrderEventListener @Autowired constructor(
    private val inventoryConfigRepository: InventoryConfigRepository
) {

    private val logger = LoggerFactory.getLogger(InventoryOrderEventListener::class.java)

    /**
     * Listen to order status changes for monitoring/auditing
     */
    @Async
    @EventListener
    fun handleOrderStatusChanged(event: OrderStatusChangedEvent) {
        try {
            logger.info(
                "Order status changed: ${event.orderNumber} " +
                "from ${event.oldStatus} to ${event.newStatus} " +
                "(Order ID: ${event.entityId}, Workspace: ${event.workspaceId})"
            )

            // Get inventory configuration
            val config = inventoryConfigRepository.findFirstByOrderByCreatedAtDesc()
            if (config?.autoDeductOnOrder == true) {
                // Log that auto-deduction is enabled
                // Actual deduction should happen in Order Service before status change
                when (event.newStatus) {
                    "CONFIRMED", "PROCESSING" -> {
                        logger.info("Auto-deduction enabled - Order Service should have deducted inventory for order ${event.orderNumber}")
                    }
                    "CANCELLED", "REFUNDED" -> {
                        logger.info("Auto-deduction enabled - Order Service should restore inventory for order ${event.orderNumber}")
                    }
                }
            }

        } catch (ex: Exception) {
            logger.error("Error processing order status change event for order ${event.orderNumber}: ${ex.message}", ex)
        }
    }

    /**
     * Listen to order creation for monitoring
     */
    @Async
    @EventListener
    fun handleOrderCreated(event: OrderCreatedEvent) {
        try {
            logger.info(
                "Order created: ${event.orderNumber} " +
                "(Customer: ${event.customerName}, Amount: ${event.totalAmount}, " +
                "Order ID: ${event.entityId}, Workspace: ${event.workspaceId})"
            )

            // Log configuration status
            val config = inventoryConfigRepository.findFirstByOrderByCreatedAtDesc()
            if (config != null) {
                logger.debug("Inventory configuration - Auto-deduct: ${config.autoDeductOnOrder}, Block when out of stock: ${config.blockOrdersWhenOutOfStock}")
            }

        } catch (ex: Exception) {
            logger.error("Error processing order creation event for order ${event.orderNumber}: ${ex.message}", ex)
        }
    }
}
