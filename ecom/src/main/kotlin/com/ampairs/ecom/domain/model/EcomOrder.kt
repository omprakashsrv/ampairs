package com.ampairs.ecom.domain.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.ecom.domain.enums.EcomOrderStatus
import jakarta.persistence.*
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant

@NamedEntityGraph(
    name = "EcomOrder.withItems",
    attributeNodes = [NamedAttributeNode("lineItems")]
)
@Entity
@Table(
    name = "ecom_order",
    indexes = [
        Index(name = "idx_ecom_order_ref",        columnList = "ecom_order_ref"),
        Index(name = "idx_ecom_order_storefront",  columnList = "storefront_id"),
        Index(name = "idx_ecom_order_customer",    columnList = "customer_id"),
        Index(name = "idx_ecom_order_workspace",   columnList = "workspace_id"),
        Index(name = "idx_ecom_order_status",      columnList = "status"),
        Index(name = "idx_ecom_order_source_cart_token", columnList = "source_cart_token"),
    ]
)
class EcomOrder : BaseDomain() {

    @Column(name = "ecom_order_ref", nullable = false, length = 50, unique = true)
    var ecomOrderRef: String = ""

    @Column(name = "storefront_id", nullable = false, length = 200)
    var storefrontId: String = ""

    @Column(name = "workspace_id", nullable = false, length = 200)
    var workspaceId: String = ""

    @Column(name = "customer_id", nullable = false, length = 200)
    var customerId: String = ""

    @Column(name = "customer_name", nullable = false, length = 255)
    var customerName: String = ""

    @Column(name = "customer_email", nullable = false, length = 320)
    var customerEmail: String = ""

    @Column(name = "customer_phone", length = 20)
    var customerPhone: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "delivery_address", nullable = false, columnDefinition = "jsonb")
    var deliveryAddress: Map<String, Any> = emptyMap()

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var status: EcomOrderStatus = EcomOrderStatus.PLACED

    @Column(name = "management_order_ref", length = 255)
    var managementOrderRef: String? = null

    // The cart session token this order was created from. Lets a retried checkout return the
    // existing order instead of creating a duplicate (idempotency).
    @Column(name = "source_cart_token", length = 200)
    var sourceCartToken: String? = null

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 4)
    var subtotal: BigDecimal = BigDecimal.ZERO

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    var totalAmount: BigDecimal = BigDecimal.ZERO

    // ── promotion-application snapshot (015) — captured at checkout; immune to later offer edits ──
    @Column(name = "promotion_discount_minor")
    var promotionDiscountMinor: Long? = null

    /** JSON array of the applied offers (uid/name/reward/discount) the engine fired at checkout. */
    @Column(name = "applied_promotions_json", columnDefinition = "TEXT")
    var appliedPromotionsJson: String? = null

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null

    @Column(name = "placed_at", nullable = false)
    var placedAt: Instant = Instant.now()

    @Column(name = "confirmed_at")
    var confirmedAt: Instant? = null

    @Column(name = "merchant_reviewed_at")
    var merchantReviewedAt: Instant? = null

    @BatchSize(size = 30)
    @OneToMany
    @JoinColumn(name = "ecom_order_id", referencedColumnName = "uid", insertable = false, updatable = false)
    var lineItems: MutableList<EcomOrderLineItem> = mutableListOf()

    override fun obtainSeqIdPrefix(): String = "ECO"
}
