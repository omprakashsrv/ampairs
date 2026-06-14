package com.ampairs.order.domain.model

import com.ampairs.core.domain.model.Address
import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.order.config.Constants
import com.ampairs.order.domain.dto.Discount
import com.ampairs.order.domain.dto.TaxInfo
import com.ampairs.order.domain.enums.OrderStatus
import jakarta.persistence.*
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant


@NamedEntityGraph(
    name = "Order.withItems",
    attributeNodes = [NamedAttributeNode("orderItems")]
)
@Entity(name = "customer_order")
@Table(
    indexes = [
        Index(name = "idx_order_uid", columnList = "uid", unique = true),
        Index(name = "order_ref_idx", columnList = "ref_id", unique = true)
    ]
)
class Order : OwnableBaseDomain() {

    @Column(name = "order_number", nullable = false, length = 255)
    var orderNumber: String = ""

    @Column(name = "order_type", nullable = false, length = 20)
    var orderType: String = "REGULAR"

    @Column(name = "customer_id", length = 36)
    var customerId: String? = null

    @Column(name = "customer_name", length = 255)
    var customerName: String? = null

    @Column(name = "customer_phone", length = 20)
    var customerPhone: String? = null

    @Column(name = "is_walk_in", nullable = false)
    var isWalkIn: Boolean = false

    @Column(name = "payment_method", length = 20)
    var paymentMethod: String = "CASH"

    @Column(name = "invoice_ref_id", nullable = true, length = 255)
    var invoiceRefId: String? = null

    @Column(name = "order_date", nullable = false)
    var orderDate: Instant = Instant.now()

    @Column(name = "delivery_date")
    var deliveryDate: Instant? = null

    @Column(name = "customer_gst", nullable = false, length = 30)
    var customerGst: String = ""

    // Seller (issuing business) identity snapshotted at issue time so the document is self-contained
    // and immutable. Name/address come from the business profile; GSTIN from the tax registration.
    @Column(name = "seller_name", length = 255)
    var sellerName: String? = null

    @Column(name = "seller_address", length = 1000)
    var sellerAddress: String? = null

    @Column(name = "seller_gst", length = 30)
    var sellerGst: String? = null

    // Place of supply (buyer/destination state) and the seller's (origin state). IGST applies iff
    // they differ; otherwise CGST+SGST. Both are snapshotted so the scenario is a pure compare at
    // edit — no GSTIN diff, no business-module access.
    @Column(name = "place_of_supply", nullable = false, length = 255)
    var placeOfSupply: String = ""

    @Column(name = "seller_place_of_supply", length = 255)
    var sellerPlaceOfSupply: String? = null

    @Column(name = "subtotal", nullable = false)
    var subtotal: Double = 0.0

    @Column(name = "discount_amount", nullable = false)
    var discountAmount: Double = 0.0

    @Column(name = "tax_amount", nullable = false)
    var taxAmount: Double = 0.0

    @Column(name = "total_amount", nullable = false)
    var totalAmount: Double = 0.0

    @Column(name = "total_cost", nullable = false)
    var totalCost: Double = 0.0

    @Column(name = "base_price", nullable = false)
    var basePrice: Double = 0.0

    @Column(name = "total_tax", nullable = false)
    var totalTax: Double = 0.0

    @Column(name = "price_mode", nullable = false, length = 20)
    var priceMode: String = "TAX_EXCLUSIVE"

    @Column(name = "overall_discount_mode", nullable = false, length = 30)
    var overallDiscountMode: String = "POST_TAX_REDUCTION"

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    var internalNotes: String? = null

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.DRAFT

    @Column(name = "total_items", nullable = false)
    var totalItems: Int = 0

    @Column(name = "total_quantity", nullable = false)
    var totalQuantity: Double = 0.0

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "billing_address", nullable = false)
    var billingAddress: Address = Address()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_address", nullable = false)
    var shippingAddress: Address = Address()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "discount", nullable = true)
    var discount: List<Discount>? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tax_info", length = 255)
    var taxInfos: List<TaxInfo> = listOf()

    /**
     * Retail business-specific order attributes stored as JSON
     * Examples:
     * - JEWELRY: design_requirements, metal_preferences, delivery_instructions
     * - KIRANA: delivery_slot, bulk_order_details, credit_terms
     * - HARDWARE: project_details, delivery_address, special_handling
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes")
    var attributes: Map<String, Any> = emptyMap()

    @Column(name = "ecom_order_ref", length = 50)
    var ecomOrderRef: String? = null

    @BatchSize(size = 30)
    @OneToMany()
    @JoinColumn(
        name = "order_id", referencedColumnName = "uid", insertable = false, updatable = false, nullable = false
    )
    var orderItems: MutableList<OrderItem> = mutableListOf()

    override fun obtainSeqIdPrefix(): String {
        return Constants.ORDER_PREFIX
    }

    /**
     * Calculate totals based on order items.
     *
     * `subtotal` is the sum of line totals, and each `OrderItem.lineTotal` is already net of that
     * line's `discountAmount` (see [OrderItem.calculateLineTotal]). Therefore item discounts must
     * NOT be subtracted again here — only the order-level `discountAmount` applies on top of the
     * (already net) subtotal. Subtracting `itemDiscounts` a second time double-counted every line
     * discount (a 100 item with a 10 line discount yielded 80 instead of 90).
     *
     * Note: this helper is not on the persistence path today — the /sync upsert trusts the totals
     * the client computes and sends — but it stays correct for any in-process order assembly.
     */
    fun calculateTotals() {
        subtotal = orderItems.sumOf { it.lineTotal }
        taxAmount = orderItems.sumOf { it.totalTax }
        totalAmount = subtotal - discountAmount + taxAmount
    }

    /**
     * Check if order can be modified
     */
    fun canBeModified(): Boolean {
        return status == OrderStatus.DRAFT
    }

    /**
     * Check if order can be cancelled
     */
    fun canBeCancelled(): Boolean {
        return status != OrderStatus.DELIVERED && status != OrderStatus.CANCELLED
    }

    /**
     * Get display-friendly order number
     */
    fun getDisplayOrderNumber(): String {
        return orderNumber.takeIf { it.isNotBlank() } ?: "ORD-${uid.takeLast(6).uppercase()}"
    }

    /**
     * Check if order is for walk-in customer
     */
    fun isWalkInOrder(): Boolean {
        return customerId == null || isWalkIn
    }

    /**
     * Add order item and recalculate totals
     */
    fun addItem(item: OrderItem) {
        item.orderId = this.uid
        orderItems.add(item)
        totalItems = orderItems.size
        totalQuantity = orderItems.sumOf { it.quantity }
        calculateTotals()
    }

    /**
     * Remove order item and recalculate totals
     */
    fun removeItem(itemId: String): Boolean {
        val removed = orderItems.removeIf { it.uid == itemId }
        if (removed) {
            totalItems = orderItems.size
            totalQuantity = orderItems.sumOf { it.quantity }
            calculateTotals()
        }
        return removed
    }
}

