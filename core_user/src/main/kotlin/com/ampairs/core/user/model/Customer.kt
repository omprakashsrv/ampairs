package com.ampairs.core.user.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.core.user.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import org.springframework.data.geo.Point

@Entity(name = "customer")
class Customer : OwnableBaseDomain() {

    @Column(name = "country_code", nullable = false)
    var countryCode: Int = 91

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "phone", nullable = false, length = 12)
    var phone: String = ""

    @Column(name = "email", length = 255)
    var email: String = ""

    @Column(name = "gstin", length = 100)
    var gstin: String = ""

    @Column(name = "address", length = 255)
    var address: String = ""

    @Column(name = "pincode", length = 10)
    var pincode: String = ""

    @Column(name = "state", length = 20)
    var state: String = ""

    @Column(name = "location")
    var location: Point? = null

    override fun obtainIdPrefix(): String {
        return Constants.CUSTOMER_PREFIX
    }
}