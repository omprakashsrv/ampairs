package com.ampairs.ecom.domain.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.ecom.domain.enums.EcomLineItemStatus
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(
    name = "ecom_order_line_item",
    indexes = [Index(name = "idx_ecom_order_line_item_order", columnList = "ecom_order_id")]
)
class EcomOrderLineItem : BaseDomain() {

    @Column(name = "ecom_order_id", nullable = false, length = 200)
    var ecomOrderId: String = ""

    @Column(name = "listed_product_id", nullable = false, length = 200)
    var listedProductId: String = ""

    @Column(name = "management_product_id", nullable = false, length = 200)
    var managementProductId: String = ""

    @Column(name = "product_name", nullable = false, length = 255)
    var productName: String = ""

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    var unitPrice: BigDecimal = BigDecimal.ZERO

    @Column(name = "quantity_ordered", nullable = false)
    var quantityOrdered: Int = 0

    @Column(name = "quantity_confirmed")
    var quantityConfirmed: Int? = null

    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    var lineTotal: BigDecimal = BigDecimal.ZERO

    // ── price-resolution snapshot (009) — copied from the cart item at checkout ──
    @Column(name = "resolved_unit_price_minor")
    var resolvedUnitPriceMinor: Long? = null

    @Column(name = "currency", length = 3)
    var currency: String? = null

    @Column(name = "price_source", length = 30)
    var priceSource: String? = null

    @Column(name = "matched_price_list_uid", length = 200)
    var matchedPriceListUid: String? = null

    @Column(name = "applied_tier_min_qty", precision = 19, scale = 4)
    var appliedTierMinQty: BigDecimal? = null

    @Column(name = "below_moq")
    var belowMoq: Boolean? = null

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: EcomLineItemStatus = EcomLineItemStatus.ORDERED

    @Column(name = "shipment_group", length = 100)
    var shipmentGroup: String? = null

    override fun obtainSeqIdPrefix(): String = "ELI"
}
