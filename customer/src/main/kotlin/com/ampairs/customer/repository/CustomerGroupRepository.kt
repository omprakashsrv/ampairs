package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.CustomerGroup
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Repository for managing workspace customer groups.
 * Uses @TenantId automatic filtering based on current workspace context.
 */
@Repository
interface CustomerGroupRepository : JpaRepository<CustomerGroup, String>, JpaSpecificationExecutor<CustomerGroup> {

    /**
     * Check if UID exists
     * Note: UID uniqueness is global (not workspace-specific)
     */
    fun existsByUid(uid: String): Boolean

    /** Connector match keys (spec 013): match an existing group by external id or uid. */
    fun findByRefId(refId: String?): CustomerGroup?
    fun findByUid(uid: String?): CustomerGroup?

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(g.updatedAt) FROM CustomerGroup g")
    fun findMaxUpdatedAt(): Instant?

    /**
     * Incremental sync feed: all groups updated at/after lastSync, INCLUDING inactive
     * (soft-deleted) rows so clients can detect deletions. Does NOT filter on active.
     * Note: @TenantId automatically filters by current workspace.
     */
    @Query("SELECT g FROM CustomerGroup g WHERE g.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(lastSync: Instant, pageable: Pageable): Page<CustomerGroup>
}
