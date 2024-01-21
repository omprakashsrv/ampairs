package com.ampairs.core.domain.model

import ch.qos.logback.core.model.Model
import com.ampairs.core.utils.Helper
import jakarta.persistence.*

@MappedSuperclass
abstract class BaseDomain : Model() {

    @Column(name = "id", length = 200, updatable = false, nullable = false, unique = true)
    var id: String = ""

    @Id
    @Column(
        name = "seq_id",
        unique = true,
        updatable = false,
        insertable = false,
        columnDefinition = "BIGINT AUTO_INCREMENT"
    )
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var seqId: Int? = null

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
    abstract fun obtainIdPrefix(): String

    @PrePersist
    protected fun prePersist() {
        if (id == "") {
            id = Helper.generateUniqueId(obtainIdPrefix(), com.ampairs.core.config.Constants.ID_LENGTH)
        }
        lastUpdated = System.currentTimeMillis()
    }

    @PreUpdate
    protected fun preUpdate() {
        lastUpdated = System.currentTimeMillis()
    }


}
