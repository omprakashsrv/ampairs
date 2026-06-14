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

/**
 * Native-SQL upsert path for `workspace_events`.
 *
 * The collapsed model holds at most one row per `(workspace_id, entity_type)`. Every event
 * UPSERTs that row with the latest `entity_id`, `event_type`, sequence number, and payload —
 * preserving the original `uid` and `created_at`. The per-workspace sequence is vended
 * atomically from `workspace_event_sequence` (UPDATE … RETURNING), which replaces the racy
 * `MAX(seq)+1` lookup that was tripping `uk_workspace_sequence` in production.
 */
@Component
class WorkspaceEventStore(
    @PersistenceContext private val em: EntityManager,
) {

    @Transactional
    fun nextSequence(workspaceId: String): Long {
        em.createNativeQuery(SQL_SEED_SEQUENCE)
            .setParameter("wid", workspaceId)
            .executeUpdate()
        val result = em.createNativeQuery(SQL_BUMP_SEQUENCE)
            .setParameter("wid", workspaceId)
            .singleResult
        return (result as Number).toLong()
    }

    @Transactional
    fun upsertWatermark(
        workspaceId: String,
        eventType: EventType,
        entityType: String,
        entityId: String,
        payload: String,
        deviceId: String,
        userId: String,
        sequenceNumber: Long,
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
            .setParameter("seq", sequenceNumber)
            .singleResult as Array<*>
        return UpsertResult(
            uid = row[0] as String,
            createdAt = (row[1] as java.sql.Timestamp).toInstant(),
        )
    }

    data class UpsertResult(val uid: String, val createdAt: Instant)

    companion object {
        private const val SQL_SEED_SEQUENCE = """
            INSERT INTO workspace_event_sequence (workspace_id, current_seq)
            VALUES (:wid, 0)
            ON CONFLICT (workspace_id) DO NOTHING
        """

        private const val SQL_BUMP_SEQUENCE = """
            UPDATE workspace_event_sequence
            SET current_seq = current_seq + 1,
                updated_at  = now()
            WHERE workspace_id = :wid
            RETURNING current_seq
        """

        private const val SQL_UPSERT_WATERMARK = """
            INSERT INTO workspace_events (
                uid, workspace_id, event_type, entity_type, entity_id, payload,
                device_id, user_id, sequence_number, owner_id, created_at, updated_at, last_updated
            ) VALUES (
                :uid, :wid, :et, :ent, :eid, :pl,
                :did, :usr, :seq, :wid, now(), now(), 0
            )
            ON CONFLICT (workspace_id, entity_type) DO UPDATE
            SET event_type      = EXCLUDED.event_type,
                entity_id       = EXCLUDED.entity_id,
                payload         = EXCLUDED.payload,
                device_id       = EXCLUDED.device_id,
                user_id         = EXCLUDED.user_id,
                sequence_number = EXCLUDED.sequence_number,
                updated_at      = now()
            RETURNING uid, created_at
        """
    }
}
