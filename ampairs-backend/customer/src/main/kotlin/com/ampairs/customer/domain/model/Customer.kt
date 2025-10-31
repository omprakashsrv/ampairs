package com.ampairs.customer.domain.model

import com.ampairs.core.domain.model.Address
import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.customer.config.Constants
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.Type
import org.hibernate.type.SqlTypes
import org.springframework.data.geo.Point

@Entity(name = "customer")
class Customer : OwnableBaseDomain() {

    @Column(name = "country_code", nullable = false)
    var countryCode: Int = 91

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "customer_type", nullable = false, length = 100)
    var customerType: String = ""

    @Column(name = "customer_group", nullable = false, length = 100)
    var customerGroup: String = ""

    @Column(name = "phone", nullable = false, length = 20)
    var phone: String = ""

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "ACTIVE"

    @Column(name = "landline", nullable = false, length = 12)
    var landline: String = ""

    @Column(name = "email", length = 255, nullable = false)
    var email: String = ""

    @Column(name = "gst_number", length = 15)
    var gstNumber: String? = null

    @Column(name = "pan_number", length = 10)
    var panNumber: String? = null

    @Column(name = "credit_limit", nullable = false)
    var creditLimit: Double = 0.0

    @Column(name = "credit_days", nullable = false)
    var creditDays: Int = 0

    @Column(name = "outstanding_amount", nullable = false)
    var outstandingAmount: Double = 0.0

    @Column(name = "address", length = 255, nullable = false)
    var address: String = ""

    @Column(name = "street", length = 255, nullable = false)
    var street: String = ""

    @Column(name = "street2", length = 255, nullable = false)
    var street2: String = ""

    @Column(name = "city", length = 255, nullable = false)
    var city: String = ""

    @Column(name = "pincode", length = 10, nullable = false)
    var pincode: String = ""

    @Column(name = "state", length = 20, nullable = false)
    var state: String = ""

    @Column(name = "country", length = 20, nullable = false)
    var country: String = "India"

    @JdbcTypeCode(SqlTypes.OTHER)
    @Column(name = "location", columnDefinition = "point")
    var location: Point? = null

    @Type(JsonType::class)
    @Column(name = "billing_address", nullable = false)
    var billingAddress: Address = Address()

    @Type(JsonType::class)
    @Column(name = "shipping_address", nullable = false)
    var shippingAddress: Address = Address()

    /**
     * Retail business-specific customer attributes stored as JSON
     * Examples:
     * - JEWELRY: preferred_metal, preferred_purity, design_preferences, size_preferences
     * - KIRANA: preferred_delivery_time, bulk_order_discount, payment_terms
     * - HARDWARE: project_types, contractor_license, preferred_brands
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes")
    var attributes: Map<String, Any>? = null

    /**
     * Customer images relationship
     * Lazy loading to avoid N+1 queries. Use @EntityGraph when needed.
     */
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_uid", referencedColumnName = "uid")
    @OrderBy("displayOrder ASC, createdAt ASC")
    var images: MutableSet<CustomerImage> = mutableSetOf()

    override fun obtainSeqIdPrefix(): String {
        return Constants.CUSTOMER_PREFIX
    }

    /**
     * Validate GST number format (Indian GST format: 15 characters)
     */
    fun isValidGstNumber(): Boolean {
        return gstNumber?.matches(Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")) ?: true // null is valid (optional field)
    }

    /**
     * Check if customer has available credit
     */
    fun hasAvailableCredit(amount: Double): Boolean {
        return (creditLimit - outstandingAmount) >= amount
    }

    /**
     * Get available credit limit
     */
    fun getAvailableCredit(): Double {
        return maxOf(0.0, creditLimit - outstandingAmount)
    }

    /**
     * Update outstanding amount
     */
    fun addToOutstanding(amount: Double) {
        outstandingAmount += amount
    }

    /**
     * Reduce outstanding amount (payment received)
     */
    fun reduceOutstanding(amount: Double) {
        outstandingAmount = maxOf(0.0, outstandingAmount - amount)
    }

    /**
     * Get the primary image for this customer
     */
    fun getPrimaryImage(): CustomerImage? {
        return images.find { it.isPrimary && it.active }
    }

    /**
     * Get all active images for this customer
     */
    fun getActiveImages(): List<CustomerImage> {
        return images.filter { it.active }.sortedWith(
            compareBy<CustomerImage> { it.displayOrder }.thenBy { it.createdAt }
        )
    }

    /**
     * Check if customer has any images
     */
    fun hasImages(): Boolean {
        return images.any { it.active }
    }

    /**
     * Get total count of active images
     */
    fun getImageCount(): Int {
        return images.count { it.active }
    }

    /**
     * Get primary image URL or null if no primary image
     */
    fun getPrimaryImageUrl(): String? {
        return getPrimaryImage()?.storageUrl
    }

}

