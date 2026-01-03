package com.ampairs.subscription.domain.service

import com.ampairs.subscription.domain.model.UsageMetric
import com.ampairs.subscription.domain.repository.UsageMetricRepository
import com.ampairs.subscription.listener.ResourceType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Service for tracking and updating resource usage metrics.
 * Called by event listeners when resources are created/deleted.
 */
@Service
@Transactional
class UsageTrackingService(
    private val usageMetricRepository: UsageMetricRepository
) {
    private val logger = LoggerFactory.getLogger(UsageTrackingService::class.java)

    /**
     * Increment the count for a resource type
     */
    fun incrementCount(workspaceId: String, resourceType: ResourceType) {
        val metric = getOrCreateCurrentPeriodMetric(workspaceId)

        when (resourceType) {
            ResourceType.CUSTOMER -> metric.customerCount++
            ResourceType.PRODUCT -> metric.productCount++
            ResourceType.INVOICE -> metric.invoiceCount++
            ResourceType.ORDER -> metric.orderCount++
            ResourceType.MEMBER -> metric.memberCount++
            ResourceType.DEVICE -> metric.deviceCount++
        }

        metric.lastCalculatedAt = Instant.now()
        usageMetricRepository.save(metric)

        logger.debug("Incremented {} count for workspace {}: new count = {}",
            resourceType, workspaceId, getCount(metric, resourceType))
    }

    /**
     * Decrement the count for a resource type
     */
    fun decrementCount(workspaceId: String, resourceType: ResourceType) {
        val metric = getOrCreateCurrentPeriodMetric(workspaceId)

        when (resourceType) {
            ResourceType.CUSTOMER -> metric.customerCount = maxOf(0, metric.customerCount - 1)
            ResourceType.PRODUCT -> metric.productCount = maxOf(0, metric.productCount - 1)
            ResourceType.INVOICE -> metric.invoiceCount = maxOf(0, metric.invoiceCount - 1)
            ResourceType.ORDER -> metric.orderCount = maxOf(0, metric.orderCount - 1)
            ResourceType.MEMBER -> metric.memberCount = maxOf(0, metric.memberCount - 1)
            ResourceType.DEVICE -> metric.deviceCount = maxOf(0, metric.deviceCount - 1)
        }

        metric.lastCalculatedAt = Instant.now()
        usageMetricRepository.save(metric)

        logger.debug("Decremented {} count for workspace {}: new count = {}",
            resourceType, workspaceId, getCount(metric, resourceType))
    }

    /**
     * Set the absolute count for a resource type (used for sync/recalculation)
     */
    fun setCount(workspaceId: String, resourceType: ResourceType, count: Int) {
        val metric = getOrCreateCurrentPeriodMetric(workspaceId)

        when (resourceType) {
            ResourceType.CUSTOMER -> metric.customerCount = count
            ResourceType.PRODUCT -> metric.productCount = count
            ResourceType.INVOICE -> metric.invoiceCount = count
            ResourceType.ORDER -> metric.orderCount = count
            ResourceType.MEMBER -> metric.memberCount = count
            ResourceType.DEVICE -> metric.deviceCount = count
        }

        metric.lastCalculatedAt = Instant.now()
        usageMetricRepository.save(metric)
    }

    /**
     * Update storage usage in bytes
     */
    fun updateStorageUsage(workspaceId: String, storageBytes: Long) {
        val metric = getOrCreateCurrentPeriodMetric(workspaceId)
        metric.storageUsedBytes = storageBytes
        metric.lastCalculatedAt = Instant.now()
        usageMetricRepository.save(metric)
    }

    /**
     * Increment API call counter
     */
    fun incrementApiCalls(workspaceId: String, count: Long = 1) {
        val metric = getOrCreateCurrentPeriodMetric(workspaceId)
        metric.apiCalls += count
        usageMetricRepository.save(metric)
    }

    /**
     * Get current count for a resource type
     */
    fun getCount(workspaceId: String, resourceType: ResourceType): Int {
        val metric = getCurrentPeriodMetric(workspaceId) ?: return 0
        return getCount(metric, resourceType)
    }

    /**
     * Get or create the usage metric for the current month
     */
    private fun getOrCreateCurrentPeriodMetric(workspaceId: String): UsageMetric {
        val now = YearMonth.now(ZoneOffset.UTC)
        val year = now.year
        val month = now.monthValue

        return usageMetricRepository.findByWorkspaceIdAndPeriodYearAndPeriodMonth(workspaceId, year, month)
            ?: UsageMetric().apply {
                this.workspaceId = workspaceId
                this.periodYear = year
                this.periodMonth = month
                usageMetricRepository.save(this)
            }
    }

    /**
     * Get the usage metric for the current month (without creating)
     */
    private fun getCurrentPeriodMetric(workspaceId: String): UsageMetric? {
        val now = YearMonth.now(ZoneOffset.UTC)
        return usageMetricRepository.findByWorkspaceIdAndPeriodYearAndPeriodMonth(workspaceId, now.year, now.monthValue)
    }

    private fun getCount(metric: UsageMetric, resourceType: ResourceType): Int {
        return when (resourceType) {
            ResourceType.CUSTOMER -> metric.customerCount
            ResourceType.PRODUCT -> metric.productCount
            ResourceType.INVOICE -> metric.invoiceCount
            ResourceType.ORDER -> metric.orderCount
            ResourceType.MEMBER -> metric.memberCount
            ResourceType.DEVICE -> metric.deviceCount
        }
    }

    /**
     * Reset monthly usage counters for all workspaces.
     *
     * Creates new usage metrics for the current month with:
     * - Cumulative counts carried forward (customers, products, members, devices, storage)
     * - Monthly counts reset to 0 (invoices, orders, API calls, SMS, emails)
     *
     * @return Number of workspaces processed
     */
    fun resetMonthlyCounters(): Int {
        val now = YearMonth.now(ZoneOffset.UTC)
        val previousMonth = now.minusMonths(1)

        logger.info("Resetting monthly usage counters for {}", now)

        // Get all usage metrics from previous month
        val previousMetrics = usageMetricRepository.findByPeriodYearAndPeriodMonth(
            previousMonth.year,
            previousMonth.monthValue
        )

        var processedCount = 0

        previousMetrics.forEach { previousMetric ->
            try {
                // Check if current month metric already exists
                val existingMetric = usageMetricRepository.findByWorkspaceIdAndPeriodYearAndPeriodMonth(
                    previousMetric.workspaceId,
                    now.year,
                    now.monthValue
                )

                if (existingMetric != null) {
                    logger.warn(
                        "Usage metric already exists for workspace {} in {}, skipping",
                        previousMetric.workspaceId, now
                    )
                    return@forEach
                }

                // Create new metric for current month
                val newMetric = UsageMetric().apply {
                    workspaceId = previousMetric.workspaceId
                    periodYear = now.year
                    periodMonth = now.monthValue

                    // Carry forward cumulative counts
                    customerCount = previousMetric.customerCount
                    productCount = previousMetric.productCount
                    memberCount = previousMetric.memberCount
                    deviceCount = previousMetric.deviceCount
                    storageUsedBytes = previousMetric.storageUsedBytes

                    // Reset monthly counts
                    invoiceCount = 0
                    orderCount = 0
                    apiCalls = 0
                    smsCount = 0
                    emailCount = 0

                    // Reset limit flags
                    customerLimitExceeded = false
                    productLimitExceeded = false
                    invoiceLimitExceeded = false
                    storageLimitExceeded = false
                    memberLimitExceeded = false
                    deviceLimitExceeded = false

                    lastCalculatedAt = Instant.now()
                }

                usageMetricRepository.save(newMetric)
                processedCount++

                logger.debug(
                    "Reset usage counters for workspace {}: carried forward {} customers, {} products",
                    previousMetric.workspaceId, newMetric.customerCount, newMetric.productCount
                )
            } catch (e: Exception) {
                logger.error(
                    "Failed to reset usage counters for workspace {}",
                    previousMetric.workspaceId,
                    e
                )
            }
        }

        logger.info("Monthly usage counter reset completed: {} workspaces processed", processedCount)
        return processedCount
    }

    /**
     * Delete old usage metrics beyond retention period.
     *
     * @param monthsToKeep Number of months to keep (default 12)
     * @return Number of records deleted
     */
    fun deleteOldUsageMetrics(monthsToKeep: Int = 12): Int {
        val cutoffMonth = YearMonth.now(ZoneOffset.UTC).minusMonths(monthsToKeep.toLong())

        logger.info("Deleting usage metrics older than {}", cutoffMonth)

        val oldMetrics = usageMetricRepository.findByPeriodYearLessThanOrPeriodYearAndPeriodMonthLessThan(
            cutoffMonth.year,
            cutoffMonth.year,
            cutoffMonth.monthValue
        )

        val deletedCount = oldMetrics.size
        usageMetricRepository.deleteAll(oldMetrics)

        logger.info("Deleted {} old usage metric records", deletedCount)
        return deletedCount
    }
}
