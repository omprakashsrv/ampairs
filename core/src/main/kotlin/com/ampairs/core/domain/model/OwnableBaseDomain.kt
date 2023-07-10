package com.ampairs.core.domain.model

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class OwnableBaseDomain : BaseDomain() {

    @Column(name = "owner_id", length = 200)
    var ownerId: String = ""

    @Column(name = "ref_id", length = 255)
    var refId: String? = null
}