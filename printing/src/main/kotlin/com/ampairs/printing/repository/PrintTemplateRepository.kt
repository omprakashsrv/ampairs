package com.ampairs.printing.repository

import com.ampairs.printing.domain.model.PrintTemplate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface PrintTemplateRepository : CrudRepository<PrintTemplate, Long> {

    fun findByUid(uid: String?): PrintTemplate?

    /**
     * Incremental sync feed: templates updated at/after lastSync, INCLUDING inactive
     * (soft-deleted) rows so clients can detect deletions. Does NOT filter on active.
     * @TenantId automatically filters by current workspace.
     */
    @EntityGraph("PrintTemplate.basic")
    @Query("SELECT t FROM print_template t WHERE t.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(@Param("lastSync") lastSync: Instant, pageable: Pageable): Page<PrintTemplate>

    /**
     * All templates for the current workspace, INCLUDING inactive — used by the sync feed when no
     * lastSync checkpoint is supplied. @TenantId automatically filters by current workspace.
     */
    @EntityGraph("PrintTemplate.basic")
    @Query("SELECT t FROM print_template t")
    fun findAllForSync(pageable: Pageable): Page<PrintTemplate>

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(t.updatedAt) FROM print_template t")
    fun findMaxUpdatedAt(): Instant?
}
