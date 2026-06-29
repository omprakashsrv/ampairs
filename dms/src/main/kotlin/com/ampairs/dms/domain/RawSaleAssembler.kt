package com.ampairs.dms.domain

import java.math.BigDecimal

/**
 * A distributor→retailer sale line read from a source document (invoice/order), before brand
 * attribution. `productUid` is the distributor's own product; `customerUid` is the retailer outlet.
 */
data class SaleLine(
    val distributorWorkspaceId: String,
    val productUid: String,
    val customerUid: String,
    val quantity: Double,
    val value: BigDecimal,
    val periodKey: String,
)

/**
 * Pure seam between source sale documents and [SnapshotAttributionCalculator] (option-(i) attribution,
 * FR-018a): turn each [SaleLine] into a [RawSale] by resolving the product's **brand label as of sale
 * time** (`brandLabelOf`) and the retailer's pincode (`pincodeOf`). The resolvers are injected so the
 * assembly is unit-testable; the live event listener supplies `ProductService`/`CustomerService`.
 */
object RawSaleAssembler {

    fun assemble(
        lines: List<SaleLine>,
        brandLabelOf: (productUid: String) -> String?,
        pincodeOf: (customerUid: String) -> String?,
    ): List<RawSale> = lines.map { line ->
        RawSale(
            distributorWorkspaceId = line.distributorWorkspaceId,
            productBrandLabelUid = brandLabelOf(line.productUid),
            distributorProductUid = line.productUid,
            quantity = line.quantity,
            value = line.value,
            retailerPincode = pincodeOf(line.customerUid),
            periodKey = line.periodKey,
        )
    }
}
