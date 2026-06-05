package com.ampairs.customer.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.customer.repository.CustomerGroupRepository
import com.ampairs.customer.repository.CustomerRepository
import com.ampairs.customer.repository.CustomerTypeRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the customer module's sync checkpoints (max `updatedAt` per entity) for the current
 * workspace. Queries are `@TenantId`-filtered, so they are automatically workspace-scoped.
 */
@Component
class CustomerCheckpointContributor(
    private val customerRepository: CustomerRepository,
    private val customerGroupRepository: CustomerGroupRepository,
    private val customerTypeRepository: CustomerTypeRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "customer" to customerRepository.findMaxUpdatedAt(),
        "customer_group" to customerGroupRepository.findMaxUpdatedAt(),
        "customer_type" to customerTypeRepository.findMaxUpdatedAt(),
    )
}
