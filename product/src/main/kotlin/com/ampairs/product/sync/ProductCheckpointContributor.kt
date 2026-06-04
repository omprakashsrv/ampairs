package com.ampairs.product.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.product.repository.ProductRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the product module's sync checkpoints (max `updatedAt` per entity) for the current
 * workspace. Queries are `@TenantId`-filtered, so they are automatically workspace-scoped.
 */
@Component
class ProductCheckpointContributor(
    private val productRepository: ProductRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "product" to productRepository.findMaxUpdatedAt(),
    )
}
