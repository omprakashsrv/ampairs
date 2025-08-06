package com.ampairs.workspace.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import org.springframework.data.geo.Point

@Entity(name = "workspace")
class Workspace : BaseDomain() {

    @Column(name = "country_code", nullable = false)
    var countryCode: Int = 91

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "phone", nullable = false, length = 12)
    var phone: String = ""

    @Column(name = "landline", nullable = false, length = 12)
    var landline: String = ""

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

    override fun obtainSeqIdPrefix(): String {
        return Constants.WORKSPACE_PREFIX
    }
}