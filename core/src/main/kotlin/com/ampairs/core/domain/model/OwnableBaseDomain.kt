package com.ampairs.core.domain.model

import jakarta.persistence.*

@MappedSuperclass
@Table(indexes = arrayOf(Index(name = "owner_id_idx", columnList = "owner_id")))
abstract class OwnableBaseDomain : BaseDomain() {

    @Column(name = "owner_id", length = 200, updatable = false, nullable = false)
    var ownerId: String = ""
}