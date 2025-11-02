package com.ampairs.business.repository

import com.ampairs.business.model.Business
import com.ampairs.business.model.enums.BusinessType
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Repository for Business entity operations.
 *
 * **Multi-Tenancy**:
 * - Business extends OwnableBaseDomain with @TenantId on ownerId
 * - All queries automatically filtered by current tenant context
 * - One Business per Workspace (enforced by unique constraint on ownerId)
 *
 * **Key Operations**:
 * - findByOwnerId: Get business for current workspace
 * - existsByOwnerId: Check if business exists for workspace
 */
@Repository
interface BusinessRepository : CrudRepository<Business, Long> {

    /**
     * Find business by ownerId (workspace ID).
     * Automatically filtered by @TenantId - only returns if ownerId matches current tenant.
     *
     * @param ownerId The workspace/owner ID
     * @return Business if found, null otherwise
     */
    fun findByOwnerId(ownerId: String): Business?

    /**
     * Find business by UID.
     *
     * @param uid The unique identifier
     * @return Business if found, null otherwise
     */
    fun findByUid(uid: String): Business?

    /**
     * Check if business exists for a workspace.
     *
     * @param ownerId The workspace/owner ID
     * @return true if exists, false otherwise
     */
    fun existsByOwnerId(ownerId: String): Boolean

    /**
     * Find all active businesses by type.
     * Useful for analytics and reporting.
     *
     * @param businessType The type of business
     * @param active Whether business is active
     * @return List of businesses matching criteria
     */
    fun findByBusinessTypeAndActive(businessType: BusinessType, active: Boolean): List<Business>

    /**
     * Find businesses by country.
     * Useful for regional analytics.
     *
     * @param country The country name
     * @return List of businesses in that country
     */
    fun findByCountry(country: String): List<Business>

    /**
     * Find businesses updated after a certain timestamp.
     * Useful for sync operations.
     *
     * @param lastSync The last sync timestamp
     * @return List of businesses updated after lastSync, ordered by updatedAt
     */
    @Query("SELECT b FROM Business b WHERE b.updatedAt >= :lastSync ORDER BY b.updatedAt ASC")
    fun findBusinessesUpdatedAfter(lastSync: Instant): List<Business>
}
