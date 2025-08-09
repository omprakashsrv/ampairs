package com.ampairs.core.domain.model

import com.ampairs.core.utils.Helper
import jakarta.persistence.*
import java.time.LocalDateTime

@MappedSuperclass
abstract class BaseDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    var id: Long = 0

    @Column(name = "uid", length = 200, updatable = false, nullable = false, unique = true)
    var uid: String = ""

    @Column(
        name = "created_at",
        columnDefinition = "TIMESTAMP default CURRENT_TIMESTAMP",
        insertable = false, updatable = false, nullable = false
    )
    var createdAt: LocalDateTime? = null

    @Column(
        name = "updated_at",
        columnDefinition = "TIMESTAMP default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP",
        insertable = false,
        updatable = false,
        nullable = false
    )
    var updatedAt: LocalDateTime? = null

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
        if (uid == "") {
            uid = Helper.generateUniqueId(obtainSeqIdPrefix(), com.ampairs.core.config.Constants.ID_LENGTH)
        }
        lastUpdated = System.currentTimeMillis()
    }

    @PreUpdate
    protected fun preUpdate() {
        lastUpdated = System.currentTimeMillis()
    }


}
