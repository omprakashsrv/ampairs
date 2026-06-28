package com.ampairs.product.service

import com.ampairs.product.domain.dto.ProductStandardCostRequest
import com.ampairs.product.domain.dto.ProductStandardCostResponse
import com.ampairs.product.domain.dto.applyRequest
import com.ampairs.product.domain.dto.asResponse
import com.ampairs.product.domain.model.ProductStandardCost
import com.ampairs.product.repository.ProductStandardCostRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

@Service
class ProductStandardCostServiceImpl(
    private val repository: ProductStandardCostRepository,
) : ProductStandardCostService {

    @Transactional(readOnly = true)
    override fun getAfterSync(lastSync: String?, pageable: Pageable): Page<ProductStandardCostResponse> {
        val page: Page<ProductStandardCost> =
            if (lastSync.isNullOrBlank()) {
                repository.findAllForSync(pageable)
            } else {
                try {
                    repository.findByUpdatedAtAfter(
                        Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8)), pageable,
                    )
                } catch (e: Exception) {
                    logger.warn("Invalid last_sync '{}', full sync", lastSync, e)
                    repository.findAllForSync(pageable)
                }
            }
        return page.map { it.asResponse() }
    }

    @Transactional
    override fun bulkUpsert(requests: List<ProductStandardCostRequest>): List<ProductStandardCostResponse> =
        requests.map { req ->
            val existing = req.uid?.takeIf { it.isNotBlank() }?.let { repository.findByUid(it) }
            val entity = (existing ?: ProductStandardCost()).applyRequest(req)
            repository.save(entity).asResponse()
        }

    @Transactional(readOnly = true)
    override fun resolveCost(productId: String, variantSku: String?, asOf: Instant): Double? =
        pick(repository.findByProductIdAndActiveTrue(productId), variantSku, asOf)?.costPrice

    companion object {
        private val logger = LoggerFactory.getLogger(ProductStandardCostServiceImpl::class.java)

        /**
         * Select the standard cost effective at [asOf]: filter to the effective window, prefer a
         * variant match, then base, then any, and within that pick the most-recently-effective
         * version. Mirrors the sales-side PricingResolutionServiceImpl.pickItem.
         */
        internal fun pick(
            items: List<ProductStandardCost>,
            variantSku: String?,
            asOf: Instant,
        ): ProductStandardCost? {
            val effective = items.filter { effectiveAt(it, asOf) }
            if (effective.isEmpty()) return null
            val variantMatches = if (variantSku != null) effective.filter { it.variantSku == variantSku } else emptyList()
            val baseMatches = effective.filter { it.variantSku == null }
            val pool = variantMatches.ifEmpty { baseMatches.ifEmpty { effective } }
            return pool.maxByOrNull { it.effectiveFrom ?: Instant.EPOCH }
        }

        /** effectiveFrom (inclusive) .. effectiveTo (exclusive); null bound = open on that side. */
        internal fun effectiveAt(item: ProductStandardCost, asOf: Instant): Boolean {
            item.effectiveFrom?.let { if (asOf.isBefore(it)) return false }
            item.effectiveTo?.let { if (!asOf.isBefore(it)) return false }
            return true
        }
    }
}
