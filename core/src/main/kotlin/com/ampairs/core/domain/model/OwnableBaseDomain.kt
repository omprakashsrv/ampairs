package com.ampairs.core.domain.model

import com.ampairs.core.multitenancy.TenantContext
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.TenantId

@MappedSuperclass
abstract class OwnableBaseDomain : BaseDomain() {

    @Column(name = "owner_id", length = 200)
    @TenantId
    var ownerId: String = TenantContext.getCurrentTenant()?.id ?: ""

    @Column(name = "ref_id", length = 255)
    var refId: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    @Column(name = "soft_deleted", nullable = false)
    var softDeleted: Boolean = false

}