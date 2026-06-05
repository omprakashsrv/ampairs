package com.ampairs.customer.domain.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.customer.domain.model.CustomerGroup
import com.ampairs.customer.repository.CustomerGroupRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Service for managing workspace customer groups.
 * Uses @TenantId automatic filtering based on current workspace context.
 */
@Service
@Transactional
class CustomerGroupService(
    private val customerGroupRepository: CustomerGroupRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {

    /**
     * Incremental sync feed for customer groups — returns rows with updatedAt >= lastSync,
     * INCLUDING inactive (soft-deleted) rows so clients can detect deletions.
     * Blank/null lastSync returns all rows (paginated) including inactive.
     * Note: @TenantId automatically filters by current workspace.
     */
    @Transactional(readOnly = true)
    fun getCustomerGroupsAfterSync(lastSync: String?, pageable: Pageable): Page<CustomerGroup> {
        return if (lastSync.isNullOrBlank()) {
            customerGroupRepository.findAll(pageable)
        } else {
            try {
                val decodedLastSync = URLDecoder.decode(lastSync, StandardCharsets.UTF_8)
                val lastSyncInstant = Instant.parse(decodedLastSync)
                customerGroupRepository.findByUpdatedAtAfter(lastSyncInstant, pageable)
            } catch (e: Exception) {
                customerGroupRepository.findAll(pageable)
            }
        }
    }

    /**
     * Find customer group by code within current workspace
     */
    @Transactional(readOnly = true)
    fun findByGroupCode(groupCode: String): CustomerGroup? {
        return customerGroupRepository.findAll().find {
            it.groupCode.equals(groupCode, ignoreCase = true) && it.active
        }
    }

    /**
     * Bulk upsert customer groups — creates new groups or updates existing ones by groupCode.
     */
    fun bulkUpsertCustomerGroups(groups: List<CustomerGroup>): List<CustomerGroup> {
        return groups.map { incoming ->
            val existing = findByGroupCode(incoming.groupCode)
            if (existing != null) {
                existing.name = incoming.name
                existing.description = incoming.description
                existing.displayOrder = incoming.displayOrder
                existing.active = incoming.active
                existing.defaultDiscountPercentage = incoming.defaultDiscountPercentage
                existing.priorityLevel = incoming.priorityLevel
                existing.metadata = incoming.metadata
                customerGroupRepository.save(existing)
                    .also { entityChangePublisher.updated("customer_group", it.uid) }
            } else {
                if (incoming.uid.isNotEmpty() && customerGroupRepository.existsByUid(incoming.uid)) {
                    throw IllegalArgumentException("Customer group with UID '${incoming.uid}' already exists")
                }
                customerGroupRepository.save(incoming)
                    .also { entityChangePublisher.created("customer_group", it.uid) }
            }
        }
    }
}
