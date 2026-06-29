package com.ampairs.dms.domain

import com.ampairs.dms.config.Constants
import java.math.BigDecimal

/**
 * A raw distributor→retailer sale line fed into the snapshot recompute. `productBrandLabelUid` is the
 * product's brand label **as of sale time** (point-in-time) — a later re-tag never moves history.
 */
data class RawSale(
    val distributorWorkspaceId: String,
    val productBrandLabelUid: String?,
    val distributorProductUid: String,
    val quantity: Double,
    val value: BigDecimal,
    val retailerPincode: String?,
    val periodKey: String,
)

/** A Hop-B brand-SKU identity for a distributor product. */
data class BrandSku(val brandProductUid: String, val brandSkuCode: String?)

/** An aggregated, brand-attributed secondary-sales figure (one row per key). */
data class AttributedRow(
    val brandWorkspaceId: String,
    val periodKey: String,
    val areaCode: String,
    val brandProductUid: String?,
    val brandSkuCode: String?,
    val quantity: Double,
    val value: BigDecimal,
)

/**
 * Pure two-level attribution + aggregation (FR-018a/b, FR-020a). Deterministic and recomputable:
 * same input ⇒ same output, so backdated/cancelled sources can drive a clean rebuild.
 *
 * - **Hop A**: a sale is attributed to a brand iff its as-of-sale brand label is designated for that
 *   brand (`hopA`); other-brand / untagged sales are EXCLUDED (no leakage).
 * - **Hop B**: where a confirmed mapping (`hopB`) exists the row is itemized by the brand SKU; else it
 *   falls into a single aggregated "unmapped" bucket — never dropped.
 * - **Area**: from the retailer pincode (national standard; comparable across distributors).
 */
object SnapshotAttributionCalculator {

    fun attribute(
        sales: List<RawSale>,
        hopA: Map<String, String>,            // distributor brand-label uid → brand workspace id
        hopB: Map<String, BrandSku>,          // distributor product uid → brand SKU
    ): List<AttributedRow> {
        val groups = LinkedHashMap<Key, Accumulator>()
        for (sale in sales) {
            val label = sale.productBrandLabelUid ?: continue
            val brandWorkspaceId = hopA[label] ?: continue // untagged / other-brand → excluded
            val sku = hopB[sale.distributorProductUid]
            val areaCode = sale.retailerPincode?.takeIf { it.isNotBlank() } ?: Constants.UNKNOWN_AREA
            val key = Key(brandWorkspaceId, sale.periodKey, areaCode, sku?.brandProductUid)
            val acc = groups.getOrPut(key) { Accumulator(sku?.brandSkuCode) }
            acc.quantity += sale.quantity
            acc.value = acc.value.add(sale.value)
        }
        return groups.map { (k, acc) ->
            AttributedRow(
                brandWorkspaceId = k.brandWorkspaceId,
                periodKey = k.periodKey,
                areaCode = k.areaCode,
                brandProductUid = k.brandProductUid,
                brandSkuCode = acc.brandSkuCode,
                quantity = acc.quantity,
                value = acc.value,
            )
        }
    }

    private data class Key(
        val brandWorkspaceId: String,
        val periodKey: String,
        val areaCode: String,
        val brandProductUid: String?,
    )

    private class Accumulator(val brandSkuCode: String?) {
        var quantity: Double = 0.0
        var value: BigDecimal = BigDecimal.ZERO
    }
}
