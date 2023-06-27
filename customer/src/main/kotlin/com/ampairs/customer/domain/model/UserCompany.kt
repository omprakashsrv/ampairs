package com.ampairs.customer.domain.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.customer.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne

@Entity(name = "user_company")
class UserCompany : BaseDomain() {

    @Column(name = "company_id", length = 200, updatable = false, nullable = false)
    var companyId: String = ""

    @Column(name = "user_id", length = 200, updatable = false, nullable = false)
    var userId: String = ""

    @OneToOne()
    @JoinColumn(name = "company_id", referencedColumnName = "id", updatable = false, insertable = false)
    lateinit var company: Company

    override fun obtainIdPrefix(): String {
        return Constants.USER_COMPANY_PREFIX
    }

}