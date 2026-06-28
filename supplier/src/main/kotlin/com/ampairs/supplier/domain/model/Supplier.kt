package com.ampairs.supplier.domain.model

import com.ampairs.core.domain.model.Address
import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.supplier.config.Constants
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.locationtech.jts.geom.Point

/**
 * Supplier master record — the buy-side counterparty (the party you purchase goods from),
 * mirroring [com.ampairs.customer.domain.model.Customer] on the sell side.
 */
@Entity(name = "supplier")
class Supplier : OwnableBaseDomain() {

    @Column(name = "country_code", nullable = false)
    var countryCode: Int = 91

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "supplier_type", nullable = false, length = 100)
    var supplierType: String = ""

    @Column(name = "supplier_group", nullable = false, length = 100)
    var supplierGroup: String = ""

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

    @Column(name = "location")
    var location: Point? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "billing_address", nullable = false)
    var billingAddress: Address = Address()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_address", nullable = false)
    var shippingAddress: Address = Address()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes")
    var attributes: Map<String, Any>? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.SUPPLIER_PREFIX
    }

    /**
     * Validate GST number format (Indian GST format: 15 characters). Null is valid (optional field).
     */
    fun isValidGstNumber(): Boolean {
        return gstNumber?.matches(Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")) ?: true
    }
}
