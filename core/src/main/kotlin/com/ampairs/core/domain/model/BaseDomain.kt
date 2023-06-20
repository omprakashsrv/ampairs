package com.ampairs.core.domain.model

import ch.qos.logback.core.model.Model
import com.ampairs.core.utils.Helper
import jakarta.persistence.*
import java.sql.Timestamp

@MappedSuperclass
@Table(uniqueConstraints = arrayOf(UniqueConstraint(columnNames = ["id"])))
abstract class BaseDomain : Model() {

    @Column(name = "id", length = 200, updatable = false, nullable = false)
    var id: String = ""

    @Id
    @Column(
        name = "seq_id",
        unique = true,
        insertable = false,
        updatable = false,
        columnDefinition = "BIGINT AUTO_INCREMENT"
    )
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected var seqId: Int? = null

    @Column(
        name = "created_at",
        columnDefinition = "timestamp default CURRENT_TIMESTAMP",
        insertable = false, updatable = false, nullable = false
    )
    protected var createdAt: Timestamp? = null

    @Column(
        name = "updated_at",
        columnDefinition = "timestamp default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP",
        insertable = false,
        updatable = false,
        nullable = false
    )
    protected var updatedAt: Timestamp? = null
    abstract fun obtainIdPrefix(): String?

    @PrePersist
    protected fun prePersist() {
        if (id == "") {
            id = Helper.generateUniqueId(obtainIdPrefix(), com.ampairs.core.config.Constants.ID_LENGTH)
        }
    }
}
