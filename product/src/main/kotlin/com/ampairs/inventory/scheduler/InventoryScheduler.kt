package com.ampairs.inventory.scheduler

import com.ampairs.inventory.service.InventoryBatchService
import com.ampairs.inventory.service.InventoryItemService
import com.ampairs.inventory.service.InventoryLedgerService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Inventory Scheduler
 *
 * Scheduled jobs for inventory management:
 * - Daily ledger generation
 * - Batch expiry checking
 * - Stock alerts
 *
 * Jobs run automatically on configured schedules.
 * Can also be triggered manually via controller endpoints.
 */
@Component
class InventoryScheduler @Autowired constructor(
    private val inventoryLedgerService: InventoryLedgerService,
    private val inventoryBatchService: InventoryBatchService,
    private val inventoryItemService: InventoryItemService
) {

    private val logger = LoggerFactory.getLogger(InventoryScheduler::class.java)

    // ============================================================================
    // Daily Ledger Generation
    // ============================================================================

    /**
     * Generate daily ledger for all items
     *
     * Runs at 1:00 AM daily (UTC)
     * Generates ledger for yesterday's transactions
     *
     * Cron expression: second minute hour day month weekday
     * "0 0 1 * * ?" = At 01:00:00am every day
     */
    @Scheduled(cron = "0 0 1 * * ?")
    fun generateDailyLedger() {
        logger.info("Starting daily ledger generation job...")
        try {
            val count = inventoryLedgerService.generateDailyLedgerForAllItems()
            logger.info("Daily ledger generation completed. Created/updated $count ledger entries.")
        } catch (e: Exception) {
            logger.error("Error during daily ledger generation", e)
        }
    }

    // ============================================================================
    // Batch Expiry Management
    // ============================================================================

    /**
     * Mark expired batches
     *
     * Runs at 6:00 AM daily (UTC)
     * Checks all batches and marks expired ones
     *
     * Cron expression: "0 0 6 * * ?" = At 06:00:00am every day
     */
    @Scheduled(cron = "0 0 6 * * ?")
    fun markExpiredBatches() {
        logger.info("Starting batch expiry check job...")
        try {
            val count = inventoryBatchService.markExpiredBatches()
            logger.info("Batch expiry check completed. Marked $count batches as expired.")
        } catch (e: Exception) {
            logger.error("Error during batch expiry check", e)
        }
    }

    /**
     * Send expiry alerts for batches expiring soon
     *
     * Runs at 6:30 AM daily (UTC)
     * Sends alerts for batches expiring within configured threshold
     *
     * Cron expression: "0 30 6 * * ?" = At 06:30:00am every day
     */
    @Scheduled(cron = "0 30 6 * * ?")
    fun sendExpiryAlerts() {
        logger.info("Starting batch expiry alerts job...")
        try {
            // Get batches expiring within 30 days (configurable via tenant settings)
            val alertDays = 30
            val expiringBatches = inventoryBatchService.getExpiringBatches(alertDays)

            if (expiringBatches.isNotEmpty()) {
                logger.warn(
                    "Found ${expiringBatches.size} batches expiring within $alertDays days. " +
                    "Alert notifications should be sent."
                )
                // TODO: Integrate with notification service to send alerts
                // notificationService.sendBatchExpiryAlerts(expiringBatches)
            } else {
                logger.info("No batches expiring within $alertDays days.")
            }
        } catch (e: Exception) {
            logger.error("Error during batch expiry alerts", e)
        }
    }

    // ============================================================================
    // Low Stock Alerts
    // ============================================================================

    /**
     * Check for low stock items and send alerts
     *
     * Runs at 7:00 AM daily (UTC)
     * Checks items at or below reorder level
     *
     * Cron expression: "0 0 7 * * ?" = At 07:00:00am every day
     */
    @Scheduled(cron = "0 0 7 * * ?")
    fun checkLowStockItems() {
        logger.info("Starting low stock check job...")
        try {
            val lowStockItems = inventoryItemService.getLowStockItems()
            val outOfStockItems = inventoryItemService.getOutOfStockItems()

            if (lowStockItems.isNotEmpty()) {
                logger.warn(
                    "Found ${lowStockItems.size} low stock items (at or below reorder level). " +
                    "Alert notifications should be sent."
                )
                // TODO: Integrate with notification service to send alerts
                // notificationService.sendLowStockAlerts(lowStockItems)
            }

            if (outOfStockItems.isNotEmpty()) {
                logger.error(
                    "CRITICAL: Found ${outOfStockItems.size} items out of stock (zero or negative stock). " +
                    "Immediate attention required!"
                )
                // TODO: Integrate with notification service for critical alerts
                // notificationService.sendCriticalStockAlerts(outOfStockItems)
            }

            if (lowStockItems.isEmpty() && outOfStockItems.isEmpty()) {
                logger.info("All items have adequate stock levels.")
            }
        } catch (e: Exception) {
            logger.error("Error during low stock check", e)
        }
    }
}
