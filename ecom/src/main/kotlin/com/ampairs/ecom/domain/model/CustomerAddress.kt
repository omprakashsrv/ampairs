package com.ampairs.ecom.domain.model

import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*

@Entity
@Table(
    name = "ecom_customer_address",
    indexes = [Index(name = "idx_ecom_customer_address_customer", columnList = "customer_id")]
)
class CustomerAddress : BaseDomain() {

    @Column(name = "customer_id", nullable = false, length = 200)
    var customerId: String = ""

    @Column(name = "label", length = 50)
    var label: String? = null

    @Column(name = "address_line1", nullable = false, length = 255)
    var addressLine1: String = ""

    @Column(name = "address_line2", length = 255)
    var addressLine2: String? = null

    @Column(name = "city", nullable = false, length = 100)
    var city: String = ""

    @Column(name = "state", nullable = false, length = 100)
    var state: String = ""

    @Column(name = "pin_code", nullable = false, length = 20)
    var pinCode: String = ""

    @Column(name = "country", nullable = false, length = 10)
    var country: String = "IN"

    @Column(name = "phone", length = 20)
    var phone: String? = null

    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false

    @Column(name = "latitude")
    var latitude: Double? = null

    @Column(name = "longitude")
    var longitude: Double? = null

    override fun obtainSeqIdPrefix(): String = "CAD"
}
