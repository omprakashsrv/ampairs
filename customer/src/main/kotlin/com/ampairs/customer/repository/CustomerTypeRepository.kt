package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.CustomerType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Repository for managing workspace customer types.
 * Uses @TenantId automatic filtering based on current workspace context.
 */
@Repository
interface CustomerTypeRepository : JpaRepository<CustomerType, String>, JpaSpecificationExecutor<CustomerType> {

    /**
     * Check if UID exists
     * Note: UID uniqueness is global (not workspace-specific)
     */
    fun existsByUid(uid: String): Boolean

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(t.updatedAt) FROM CustomerType t")
    fun findMaxUpdatedAt(): Instant?

    /**
     * Incremental sync feed: all types updated at/after lastSync, INCLUDING inactive
     * (soft-deleted) rows so clients can detect deletions. Does NOT filter on active.
     * Note: @TenantId automatically filters by current workspace.
     */
    @Query("SELECT t FROM CustomerType t WHERE t.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(lastSync: Instant, pageable: Pageable): Page<CustomerType>
}
