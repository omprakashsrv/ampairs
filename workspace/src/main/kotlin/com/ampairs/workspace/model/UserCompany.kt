package com.ampairs.workspace.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.workspace.model.enums.Role
import jakarta.persistence.*

@Entity(name = "user_company")
class UserWorkspace : BaseDomain() {

    @Column(name = "company_id", length = 200, updatable = false, nullable = false)
    var companyId: String = ""

    @Column(name = "user_id", length = 200, updatable = false, nullable = false)
    var userId: String = ""

    @Column(name = "role", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    var role: Role = Role.USER

    @OneToOne
    @JoinColumn(name = "company_id", referencedColumnName = "id", updatable = false, insertable = false)
    lateinit var company: Workspace

    override fun obtainSeqIdPrefix(): String {
        return Constants.USER_WORKSPACE_PREFIX
    }

}