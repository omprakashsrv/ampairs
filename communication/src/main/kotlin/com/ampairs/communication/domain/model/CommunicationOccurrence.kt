package com.ampairs.communication.domain.model

import com.ampairs.communication.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant

/**
 * At-most-once ledger for schedule occurrences. The unique `(schedule_uid, occurrence_key)` is the
 * real idempotency guard — a racing/duplicate sweeper insert fails and that occurrence is skipped.
 * Server-internal; not synced.
 */
@Entity(name = "communication_occurrence")
@Table
class CommunicationOccurrence : OwnableBaseDomain() {

    @Column(name = "schedule_uid", length = 200, nullable = false)
    var scheduleUid: String = ""

    @Column(name = "occurrence_key", length = 64, nullable = false)
    var occurrenceKey: String = ""

    @Column(name = "materialized_at", nullable = false)
    var materializedAt: Instant = Instant.now()

    override fun obtainSeqIdPrefix(): String = Constants.OCCURRENCE_PREFIX
}
