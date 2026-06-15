package com.ampairs.connector.repository

import com.ampairs.connector.domain.model.ConnectorInstallation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

/** Tenant-filtered automatically via `@TenantId ownerId`. */
interface ConnectorInstallationRepository : CrudRepository<ConnectorInstallation, Long> {

    fun findByUid(uid: String?): ConnectorInstallation?

    fun findByActiveTrue(): List<ConnectorInstallation>

    /** Used to enforce one active install per connector type per workspace (FR-005). */
    fun findByConnectorTypeAndActiveTrue(connectorType: String): ConnectorInstallation?

    @EntityGraph("ConnectorInstallation.basic")
    @Query("SELECT i FROM connector_installation i WHERE i.updatedAt > :updatedAt ORDER BY i.updatedAt ASC")
    fun findByUpdatedAtAfter(@Param("updatedAt") updatedAt: Instant, pageable: Pageable): Page<ConnectorInstallation>

    @Query("SELECT MAX(i.updatedAt) FROM connector_installation i")
    fun findMaxUpdatedAt(): Instant?
}
