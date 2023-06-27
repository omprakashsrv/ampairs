package com.ampairs.customer.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.customer.config.Constants
import jakarta.persistence.*

@Entity(name = "customer")
class Customer() : OwnableBaseDomain() {

    @Column(name = "company_id", length = 200, updatable = false, nullable = false)
    var companyId: String = ""

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(
        name = "company_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
        nullable = false
    )
    lateinit var company: Company

    override fun obtainIdPrefix(): String {
        return Constants.CUSTOMER_PREFIX
    }
}