package com.ampairs.event.repository

import com.ampairs.core.config.Constants as CoreConstants
import com.ampairs.core.utils.Helper
import com.ampairs.event.config.Constants
import com.ampairs.event.domain.EventType
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Native-SQL upsert path for `workspace_events`.
 *
 * The collapsed model holds at most one row per `(workspace_id, entity_type)`. Every event
 * UPSERTs that row with the latest `entity_id`, `event_type`, sequence number, and payload —
 * preserving the original `uid` and `created_at`. The sequence number is vended from a single
 * Postgres SEQUENCE (`workspace_event_seq`), which is atomic by definition and replaces the
 * racy `MAX(seq)+1` lookup that was tripping `uk_workspace_sequence` in production.
 *
 * Per-workspace numbers are sparse (gaps when other workspaces consume the sequence) but
 * strictly monotonic within a workspace — which is all the `?sinceSequence=N` catch-up
 * contract requires.
 */
@Component
class WorkspaceEventStore(
    @PersistenceContext private val em: EntityManager,
) {

    @Transactional
    fun upsertWatermark(
        workspaceId: String,
        eventType: EventType,
        entityType: String,
        entityId: String,
        payload: String,
        deviceId: String,
        userId: String,
    ): UpsertResult {
        val candidateUid = Helper.generateUniqueId(Constants.WORKSPACE_EVENT_PREFIX, CoreConstants.ID_LENGTH)
        val row = em.createNativeQuery(SQL_UPSERT_WATERMARK)
            .setParameter("uid", candidateUid)
            .setParameter("wid", workspaceId)
            .setParameter("et", eventType.name)
            .setParameter("ent", entityType)
            .setParameter("eid", entityId)
            .setParameter("pl", payload)
            .setParameter("did", deviceId)
            .setParameter("usr", userId)
            .singleResult as Array<*>
        return UpsertResult(
            uid = row[0] as String,
            createdAt = toInstant(row[1]),
            sequenceNumber = (row[2] as Number).toLong(),
        )
    }

    // The `created_at` column was declared TIMESTAMP WITHOUT TIME ZONE in V1.0.3 (predates the
    // project-wide TIMESTAMPTZ rule), so the driver hands back a LocalDateTime; older code paths
    // could also surface a Timestamp or OffsetDateTime depending on the column type encountered.
    private fun toInstant(value: Any?): Instant = when (value) {
        is Instant -> value
        is java.sql.Timestamp -> value.toInstant()
        is OffsetDateTime -> value.toInstant()
        is LocalDateTime -> value.toInstant(ZoneOffset.UTC)
        else -> error("Unexpected created_at type: ${value?.let { it::class.java.name } ?: "null"}")
    }

    data class UpsertResult(val uid: String, val createdAt: Instant, val sequenceNumber: Long)

    companion object {
        private const val SQL_UPSERT_WATERMARK = """
            INSERT INTO workspace_events (
                uid, workspace_id, event_type, entity_type, entity_id, payload,
                device_id, user_id, sequence_number, owner_id, created_at, updated_at, last_updated
            ) VALUES (
                :uid, :wid, :et, :ent, :eid, :pl,
                :did, :usr, nextval('workspace_event_seq'), :wid, now(), now(), 0
            )
            ON CONFLICT (workspace_id, entity_type) DO UPDATE
            SET event_type      = EXCLUDED.event_type,
                entity_id       = EXCLUDED.entity_id,
                payload         = EXCLUDED.payload,
                device_id       = EXCLUDED.device_id,
                user_id         = EXCLUDED.user_id,
                sequence_number = EXCLUDED.sequence_number,
                updated_at      = now()
            RETURNING uid, created_at, sequence_number
        """
    }
}
