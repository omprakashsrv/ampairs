package com.ampairs.sequence.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.sequence.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

enum class AllocationStatus {
    ACTIVE,
    EXHAUSTED,
    RELEASED,
}

/**
 * An exclusive, contiguous block of counter values granted to one device for one
 * sequence definition. Ranges for the same definition never overlap (the grant advances
 * the definition's high-water mark past the range under a row lock). Unconsumed
 * remainders of abandoned blocks are never reissued — gaps are acceptable, duplicates
 * are not.
 *
 * The definition's formatting fields (prefix/suffix/padding/increment) are snapshotted
 * at grant time so devices can format numbers offline without the definition row.
 */
@Entity(name = "sequence_allocation")
@Table(
    indexes = [
        Index(name = "idx_sequence_allocation_uid", columnList = "uid", unique = true),
        Index(name = "idx_sequence_allocation_owner_device", columnList = "owner_id, device_id, status"),
        Index(name = "idx_sequence_allocation_owner_definition", columnList = "owner_id, definition_uid"),
    ]
)
class SequenceAllocation : OwnableBaseDomain() {

    @Column(name = "definition_uid", length = 200, nullable = false)
    var definitionUid: String = ""

    /** Denormalized from the definition for device lookups. */
    @Column(name = "entity_type", length = 64, nullable = false)
    var entityType: String = ""

    @Column(name = "device_id", length = 200, nullable = false)
    var deviceId: String = ""

    /** Requesting user uid (audit; user-scope grants). */
    @Column(name = "user_id", length = 200)
    var userId: String? = null

    /** First value in the block (inclusive). */
    @Column(name = "range_start", nullable = false)
    var rangeStart: Long = 0

    /** Last value in the block (inclusive). */
    @Column(name = "range_end", nullable = false)
    var rangeEnd: Long = 0

    /** Device progress; = rangeStart at grant; > rangeEnd once exhausted. */
    @Column(name = "next_available", nullable = false)
    var nextAvailable: Long = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    var status: AllocationStatus = AllocationStatus.ACTIVE

    // Definition format snapshot at grant time
    @Column(name = "prefix", length = 32)
    var prefix: String? = null

    @Column(name = "suffix", length = 32)
    var suffix: String? = null

    @Column(name = "padding_length", nullable = false)
    var paddingLength: Int = 0

    @Column(name = "increment_step", nullable = false)
    var incrementStep: Int = 1

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return Constants.SEQUENCE_ALLOCATION_PREFIX
    }
}
