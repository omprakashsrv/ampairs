package com.ampairs.product.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.product.repository.ProductBrandRepository
import com.ampairs.product.repository.ProductCategoryRepository
import com.ampairs.product.repository.ProductGroupRepository
import com.ampairs.product.repository.ProductRepository
import com.ampairs.product.repository.ProductStandardCostRepository
import com.ampairs.product.repository.ProductSubCategoryRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the product module's sync checkpoints (max `updatedAt` per entity) for the current
 * workspace. `product_catalog` aggregates the catalog grouping tables (category, brand, group,
 * sub-category). Queries are `@TenantId`-filtered, so they are automatically workspace-scoped.
 */
@Component
class ProductCheckpointContributor(
    private val productRepository: ProductRepository,
    private val productCategoryRepository: ProductCategoryRepository,
    private val productBrandRepository: ProductBrandRepository,
    private val productGroupRepository: ProductGroupRepository,
    private val productSubCategoryRepository: ProductSubCategoryRepository,
    private val productStandardCostRepository: ProductStandardCostRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> {
        val maxCatalog = listOfNotNull(
            productCategoryRepository.findMaxUpdatedAt(),
            productBrandRepository.findMaxUpdatedAt(),
            productGroupRepository.findMaxUpdatedAt(),
            productSubCategoryRepository.findMaxUpdatedAt(),
        ).maxOrNull()
        return mapOf(
            "product" to productRepository.findMaxUpdatedAt(),
            "product_catalog" to maxCatalog,
            "product_standard_cost" to productStandardCostRepository.findMaxUpdatedAt(),
        )
    }
}
