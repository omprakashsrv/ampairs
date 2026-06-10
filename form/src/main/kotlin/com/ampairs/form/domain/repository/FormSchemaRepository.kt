package com.ampairs.form.domain.repository

import com.ampairs.form.domain.model.FormSchema
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Aggregate-header repository. Automatically `@TenantId`-filtered (workspace-scoped).
 * The sync feed is keyed on this header's `updatedAt` (= max across the aggregate's members,
 * maintained by the service on save).
 */
@Repository
interface FormSchemaRepository : JpaRepository<FormSchema, Long> {

    fun findByEntityType(entityType: String): FormSchema?

    fun findByUid(uid: String): FormSchema?

    /** Sync checkpoint: max updatedAt across this workspace's form schemas (null when empty). */
    @Query("SELECT MAX(s.updatedAt) FROM FormSchema s")
    fun findMaxUpdatedAt(): Instant?

    /** Incremental pull: aggregates updated at/after [lastSync]. Stable order via uid tiebreaker. */
    @Query("SELECT s FROM FormSchema s WHERE s.updatedAt >= :lastSync")
    fun findUpdatedAfter(lastSync: Instant, pageable: Pageable): Page<FormSchema>

    /** Full pull feed (no last_sync). */
    @Query("SELECT s FROM FormSchema s")
    fun findAllForSync(pageable: Pageable): Page<FormSchema>
}
