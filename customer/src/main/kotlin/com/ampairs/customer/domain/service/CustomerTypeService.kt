package com.ampairs.customer.domain.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.customer.domain.model.CustomerType
import com.ampairs.customer.repository.CustomerTypeRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Service for managing workspace customer types.
 * Uses @TenantId automatic filtering based on current workspace context.
 */
@Service
@Transactional
class CustomerTypeService(
    private val customerTypeRepository: CustomerTypeRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {

    /**
     * Incremental sync feed for customer types — returns rows with updatedAt >= lastSync,
     * INCLUDING inactive (soft-deleted) rows so clients can detect deletions.
     * Blank/null lastSync returns all rows (paginated) including inactive.
     * Note: @TenantId automatically filters by current workspace.
     */
    @Transactional(readOnly = true)
    fun getCustomerTypesAfterSync(lastSync: String?, pageable: Pageable): Page<CustomerType> {
        return if (lastSync.isNullOrBlank()) {
            customerTypeRepository.findAll(pageable)
        } else {
            try {
                val decodedLastSync = URLDecoder.decode(lastSync, StandardCharsets.UTF_8)
                val lastSyncInstant = Instant.parse(decodedLastSync)
                customerTypeRepository.findByUpdatedAtAfter(lastSyncInstant, pageable)
            } catch (e: Exception) {
                customerTypeRepository.findAll(pageable)
            }
        }
    }

    /**
     * Find customer type by code within current workspace
     */
    @Transactional(readOnly = true)
    fun findByTypeCode(typeCode: String): CustomerType? {
        return customerTypeRepository.findAll().find {
            it.typeCode.equals(typeCode, ignoreCase = true) && it.active
        }
    }

    /**
     * Bulk upsert customer types — creates new types or updates existing ones by typeCode.
     */
    fun bulkUpsertCustomerTypes(types: List<CustomerType>): List<CustomerType> {
        return types.map { incoming ->
            val existing = findByTypeCode(incoming.typeCode)
            if (existing != null) {
                existing.name = incoming.name
                existing.description = incoming.description
                existing.displayOrder = incoming.displayOrder
                existing.active = incoming.active
                existing.defaultCreditLimit = incoming.defaultCreditLimit
                existing.defaultCreditDays = incoming.defaultCreditDays
                existing.metadata = incoming.metadata
                incoming.refId?.takeIf { it.isNotBlank() }?.let { existing.refId = it }
                customerTypeRepository.save(existing)
                    .also { entityChangePublisher.updated("customer_type", it.uid) }
            } else {
                if (incoming.uid.isNotEmpty() && customerTypeRepository.existsByUid(incoming.uid)) {
                    throw IllegalArgumentException("Customer type with UID '${incoming.uid}' already exists")
                }
                customerTypeRepository.save(incoming)
                    .also { entityChangePublisher.created("customer_type", it.uid) }
            }
        }
    }
}
