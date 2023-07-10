package com.ampairs.core.domain.model

import jakarta.persistence.*

@MappedSuperclass
@Table(indexes = arrayOf(Index(name = "owner_id_idx", columnList = "owner_id")))
abstract class OwnableBaseDomain : BaseDomain() {

    @Column(name = "owner_id", length = 200)
    var ownerId: String = ""

    @Column(name = "ref_id", length = 255)
    var refId: String = ""
}