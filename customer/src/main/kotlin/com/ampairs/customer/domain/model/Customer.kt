package com.ampairs.customer.domain.model

import com.ampairs.core.domain.model.Address
import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.customer.config.Constants
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import org.springframework.data.geo.Point

@Entity(name = "customer")
@Table(indexes = arrayOf(Index(name = "customer_ref_idx", columnList = "ref_id", unique = true)))
class Customer : OwnableBaseDomain() {

    @Column(name = "country_code", nullable = false)
    var countryCode: Int = 91

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "company_id", nullable = false, length = 255)
    var companyId: String = ""

    @Column(name = "phone", nullable = false, length = 12)
    var phone: String = ""

    @Column(name = "landline", nullable = false, length = 12)
    var landline: String = ""

    @Column(name = "email", length = 255, nullable = false)
    var email: String = ""

    @Column(name = "gstin", length = 100, nullable = false)
    var gstin: String = ""

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

    @Column(name = "billingSameAsRegistered", nullable = false)
    var billingSameAsRegistered: Boolean = true

    @Column(name = "shippingSameAsBilling", nullable = false)
    var shippingSameAsBilling: Boolean = true

    @Column(name = "country", length = 20, nullable = false)
    var country: String = "India"

    @Column(name = "location")
    var location: Point? = null

    @Type(JsonType::class)
    @Column(name = "billing_address", nullable = false, columnDefinition = "json")
    var billingAddress: Address = Address()

    @Type(JsonType::class)
    @Column(name = "shipping_address", nullable = false, columnDefinition = "json")
    var shippingAddress: Address = Address()

    override fun obtainIdPrefix(): String {
        return Constants.CUSTOMER_PREFIX
    }
}