package com.ampairs.product.service

import com.ampairs.product.domain.dto.ProductStandardCostRequest
import com.ampairs.product.domain.dto.ProductStandardCostResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.Instant

/**
 * Effective-dated standard purchase cost (Tally "Standard Cost" / "Applicable From"). Standard
 * `/sync` resource plus an as-of-date resolution used to auto-fill purchase voucher line rates.
 */
interface ProductStandardCostService {

    /** Sync feed (includes soft-deleted rows). */
    fun getAfterSync(lastSync: String?, pageable: Pageable): Page<ProductStandardCostResponse>

    /** UID-keyed bulk upsert honoring the soft-delete flag. */
    fun bulkUpsert(requests: List<ProductStandardCostRequest>): List<ProductStandardCostResponse>

    /**
     * The standard cost effective for [productId] (optionally [variantSku]) as of [asOf]. Returns null
     * when no cost version applies (caller falls back to the catalog cost). Variant match wins over a
     * base entry; among applicable versions the most-recently-effective one wins.
     */
    fun resolveCost(productId: String, variantSku: String?, asOf: Instant): BigDecimal?
}
