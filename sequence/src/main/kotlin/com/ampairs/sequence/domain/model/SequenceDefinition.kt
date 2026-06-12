package com.ampairs.sequence.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.sequence.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

enum class SequenceScope {
    WORKSPACE,
    USER,
}

/**
 * A configurable numbering scheme for one entity type within a workspace.
 *
 * `currentValue` is the high-water mark: the last value issued or allocated. It is
 * monotonically non-decreasing — consumed numbers are never reused, gaps are acceptable.
 * The next value to issue is `startValue` when `currentValue < startValue`, otherwise
 * `currentValue + incrementStep` (see [com.ampairs.sequence.service.SequenceFormatter]).
 *
 * At most one `active = true` row may exist per (owner, entityType, scope, userId) —
 * enforced at the service layer (MySQL has no partial unique indexes).
 */
@Entity(name = "sequence_definition")
@Table(
    indexes = [
        Index(name = "idx_sequence_definition_uid", columnList = "uid", unique = true),
        Index(name = "idx_sequence_definition_owner_entity", columnList = "owner_id, entity_type"),
        Index(name = "idx_sequence_definition_owner_updated", columnList = "owner_id, updated_at"),
    ]
)
class SequenceDefinition : OwnableBaseDomain() {

    @Column(name = "entity_type", length = 64, nullable = false)
    var entityType: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", length = 16, nullable = false)
    var scope: SequenceScope = SequenceScope.WORKSPACE

    /** Owning user uid — required iff scope = USER. */
    @Column(name = "user_id", length = 200)
    var userId: String? = null

    @Column(name = "prefix", length = 32)
    var prefix: String? = null

    @Column(name = "suffix", length = 32)
    var suffix: String? = null

    /** 0 = no zero-padding. */
    @Column(name = "padding_length", nullable = false)
    var paddingLength: Int = 0

    @Column(name = "start_value", nullable = false)
    var startValue: Long = 1

    @Column(name = "increment_step", nullable = false)
    var incrementStep: Int = 1

    /** High-water mark: last issued/allocated value; 0 = nothing issued yet. */
    @Column(name = "current_value", nullable = false)
    var currentValue: Long = 0

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return Constants.SEQUENCE_DEFINITION_PREFIX
    }
}
