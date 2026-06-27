package com.ampairs.report.repository

import com.ampairs.report.domain.model.ExportTemplate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ExportTemplateRepository : CrudRepository<ExportTemplate, Long> {

    fun findByUid(uid: String?): ExportTemplate?

    /** Sync checkpoint: max `updatedAt` for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(t.updatedAt) FROM export_template t")
    fun findMaxUpdatedAt(): Instant?

    /**
     * Incremental sync feed: all templates updated at/after lastSync, INCLUDING inactive
     * (soft-deleted) rows so clients can detect deletions. @TenantId filters by workspace.
     */
    @EntityGraph("ExportTemplate.basic")
    @Query("SELECT t FROM export_template t WHERE t.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(@Param("lastSync") lastSync: Instant, pageable: Pageable): Page<ExportTemplate>

    /**
     * All templates for the current workspace, INCLUDING inactive — used by the sync feed when no
     * lastSync checkpoint is supplied. @TenantId filters by workspace.
     */
    @EntityGraph("ExportTemplate.basic")
    @Query("SELECT t FROM export_template t")
    fun findAllForSync(pageable: Pageable): Page<ExportTemplate>
}
