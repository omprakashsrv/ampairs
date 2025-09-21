package com.ampairs.core.domain.model

import com.ampairs.core.multitenancy.TenantContextHolder
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.TenantId

@MappedSuperclass
abstract class OwnableBaseDomain : BaseDomain() {

    @Column(name = "owner_id", length = 200)
    @TenantId
    var ownerId: String = TenantContextHolder.getCurrentTenant() ?: ""

    @Column(name = "ref_id", length = 255)
    var refId: String? = null

}