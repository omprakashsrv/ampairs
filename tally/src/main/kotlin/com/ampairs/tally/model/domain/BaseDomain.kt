package com.ampairs.tally.model.domain

import ch.qos.logback.core.model.Model
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
    )
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var seqId: Int? = null

    @Column(
        name = "created_at",
        insertable = false, updatable = false, nullable = false
    )
    lateinit var createdAt: String

    @Column(
        name = "updated_at",
        insertable = false,
        updatable = false,
        nullable = false
    )
    lateinit var updatedAt: String

    @Column(
        name = "last_updated",
        insertable = false,
        updatable = false,
        nullable = false
    )
    var lastUpdated: Long = 0

    @PrePersist
    protected fun prePersist() {
        lastUpdated = System.currentTimeMillis()
    }
}
