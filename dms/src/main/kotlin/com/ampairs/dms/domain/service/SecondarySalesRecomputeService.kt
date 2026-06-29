package com.ampairs.dms.domain.service

import com.ampairs.dms.domain.RawSaleAssembler
import com.ampairs.dms.domain.SaleLine
import com.ampairs.dms.domain.SnapshotAttributionCalculator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Orchestrates a distributor's secondary-sales snapshot recompute by composing the building blocks:
 * source [SaleLine]s → [RawSaleAssembler] (brand label + pincode resolved) → [AttributionMapProvider]
 * (Hop A/B from trade consent) → [SnapshotAttributionCalculator] → [SnapshotService] wholesale persist.
 *
 * The caller supplies the source lines + resolvers; the live event listener will pass
 * InvoiceService/OrderService lines with ProductService/CustomerService resolvers (the remaining edge).
 */
@Service
@Transactional
class SecondarySalesRecomputeService(
    private val attributionMapProvider: AttributionMapProvider,
    private val snapshotService: SnapshotService,
) {

    fun recompute(
        distributorWorkspaceId: String,
        lines: List<SaleLine>,
        brandLabelOf: (productUid: String) -> String?,
        pincodeOf: (customerUid: String) -> String?,
    ): Int {
        val rawSales = RawSaleAssembler.assemble(lines, brandLabelOf, pincodeOf)
        val hopA = attributionMapProvider.hopA(distributorWorkspaceId)
        val hopB = attributionMapProvider.hopB(distributorWorkspaceId)
        val attributed = SnapshotAttributionCalculator.attribute(rawSales, hopA, hopB)
        return snapshotService.recomputeSecondarySales(distributorWorkspaceId, attributed)
    }
}
