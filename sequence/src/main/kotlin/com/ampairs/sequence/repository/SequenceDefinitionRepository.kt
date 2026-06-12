package com.ampairs.sequence.repository

import com.ampairs.sequence.domain.model.SequenceDefinition
import com.ampairs.sequence.domain.model.SequenceScope
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface SequenceDefinitionRepository : CrudRepository<SequenceDefinition, Long> {

    fun findByUid(uid: String?): SequenceDefinition?

    /**
     * Counter advancement lock: serializes concurrent generations/block grants on the
     * definition row (SELECT … FOR UPDATE). Must be called inside a transaction.
     * Note: @TenantId automatically filters by current workspace.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM sequence_definition d WHERE d.uid = :uid")
    fun findByUidForUpdate(@Param("uid") uid: String): SequenceDefinition?

    fun findAllByOrderByActiveDescEntityTypeAsc(): List<SequenceDefinition>

    // Active-key resolution (at most one active row per key — service-enforced)
    fun findFirstByEntityTypeAndScopeAndUserIdAndActiveTrue(
        entityType: String,
        scope: SequenceScope,
        userId: String,
    ): SequenceDefinition?

    fun findFirstByEntityTypeAndScopeAndActiveTrue(
        entityType: String,
        scope: SequenceScope,
    ): SequenceDefinition?

    // Uniqueness checks for the active key
    fun existsByEntityTypeAndScopeAndUserIdAndActiveTrueAndUidNot(
        entityType: String,
        scope: SequenceScope,
        userId: String,
        uid: String,
    ): Boolean

    fun existsByEntityTypeAndScopeAndActiveTrueAndUidNot(
        entityType: String,
        scope: SequenceScope,
        uid: String,
    ): Boolean

    /**
     * Incremental sync feed: all definitions updated at/after lastSync, INCLUDING inactive
     * rows so clients can detect deactivations. Note: @TenantId filters by current workspace.
     */
    @Query("SELECT d FROM sequence_definition d WHERE d.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(@Param("lastSync") lastSync: Instant, pageable: Pageable): Page<SequenceDefinition>

    /** Full sync feed (no checkpoint), INCLUDING inactive rows. @TenantId-filtered. */
    @Query("SELECT d FROM sequence_definition d")
    fun findAllForSync(pageable: Pageable): Page<SequenceDefinition>
}
