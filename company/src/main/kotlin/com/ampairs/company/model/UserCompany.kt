package com.ampairs.company.model

import com.ampairs.company.model.enums.Role
import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*

@Entity(name = "user_company")
class UserCompany : BaseDomain() {

    @Column(name = "company_id", length = 200, updatable = false, nullable = false)
    var companyId: String = ""

    @Column(name = "user_id", length = 200, updatable = false, nullable = false)
    var userId: String = ""

    @Column(name = "role", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    var role: Role = Role.USER

    @OneToOne
    @JoinColumn(name = "company_id", referencedColumnName = "id", updatable = false, insertable = false)
    lateinit var company: Company

    override fun obtainIdPrefix(): String {
        return Constants.USER_COMPANY_PREFIX
    }

}