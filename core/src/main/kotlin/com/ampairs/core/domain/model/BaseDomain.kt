package com.ampairs.core.domain.model

import com.ampairs.core.utils.Helper
import jakarta.persistence.*

@MappedSuperclass
abstract class BaseDomain {

    @Id
    @Column(name = "id", length = 200, updatable = false, nullable = false)
    var id: String = ""

    @Column(name = "seq_id", length = 200, updatable = false, nullable = false, unique = true)
    var seqId: String = ""

    @Column(
        name = "created_at",
        columnDefinition = "TIMESTAMP default CURRENT_TIMESTAMP",
        insertable = false, updatable = false, nullable = false
    )
    var createdAt: String? = null

    @Column(
        name = "updated_at",
        columnDefinition = "TIMESTAMP default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP",
        insertable = false,
        updatable = false,
        nullable = false
    )
    var updatedAt: String? = null

    @Column(
        name = "last_updated",
        columnDefinition = "BIGINT default (UNIX_TIMESTAMP()*1000)",
        insertable = false,
        updatable = false,
        nullable = false
    )
    var lastUpdated: Long = 0
    abstract fun obtainSeqIdPrefix(): String

    @PrePersist
    protected fun prePersist() {
        if (seqId == "") {
            seqId = Helper.generateUniqueId(obtainSeqIdPrefix(), com.ampairs.core.config.Constants.ID_LENGTH)
        }
        lastUpdated = System.currentTimeMillis()
    }

    @PreUpdate
    protected fun preUpdate() {
        lastUpdated = System.currentTimeMillis()
    }


}
