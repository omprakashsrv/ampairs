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
}
