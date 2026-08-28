package com.ampairs.connector.service

import com.ampairs.connector.domain.model.ConnectorInstallation

/**
 * SPI for a SERVER-SIDE connector's sync engine — the backend analogue of the client-side
 * `TallySyncService` (which runs in the desktop app). One implementation per server-side connector
 * type (Generic HTTP/JSON today; Zoho/Shopify/QuickBooks later, each copying the generic one).
 *
 * The engine pulls from the remote system, applies the installation's stored field mapping, and writes
 * through the EXISTING connector sparse-upsert path ([ConnectorSparseUpsertService]) — so it inherits
 * the data-loss-safe presence ∩ allowlist guarantee unchanged. It advances per-entity checkpoints and
 * (via the sparse-upsert path) records a run per entity.
 *
 * [ConnectorRunScheduler] selects active SERVER_SIDE installations across tenants and calls
 * [runCycle] with tenant context already set — so implementations use the tenant-scoped services
 * normally and MUST NOT touch [com.ampairs.core.multitenancy.TenantContextHolder] themselves.
 *
 * OAuth authorize/callback/refresh and webhook receivers are out of scope for this phase (spec 029
 * FR-S08); this SPI and the encrypted secret store accommodate them without re-architecting.
 */
interface ServerSideConnectorSyncExecutor {
    /** The `CatalogueConnector.type` this executor handles (must be a SERVER_SIDE connector). */
    val connectorType: String

    /** Run one incremental sync cycle for [installation]. Tenant context is already set by the caller. */
    fun runCycle(installation: ConnectorInstallation): ServerSyncOutcome

    /**
     * Server-side reachability probe for the connection test (FR-S06): true if the remote system is
     * reachable with the stored config/credentials. Default null = this connector has no server-side
     * probe (fall back to the client-reported result). Tenant context is set by the caller.
     */
    fun testConnection(installation: ConnectorInstallation): Boolean? = null
}

/** Aggregate result of one server-side sync cycle across all of a connector's entities. */
data class ServerSyncOutcome(
    val entityOutcomes: List<EntitySyncOutcome> = emptyList(),
) {
    val processed: Int get() = entityOutcomes.sumOf { it.processed }
    val created: Int get() = entityOutcomes.sumOf { it.created }
    val updated: Int get() = entityOutcomes.sumOf { it.updated }
    val failed: Int get() = entityOutcomes.sumOf { it.failed }
    /** True when at least one entity failed to fetch/apply — surfaces to the scheduler for status. */
    val hadFailure: Boolean get() = entityOutcomes.any { it.error != null || it.failed > 0 }
}

/** Per-entity result within a cycle. [error] is set when the whole entity fetch/parse failed. */
data class EntitySyncOutcome(
    val entityType: String,
    val processed: Int = 0,
    val created: Int = 0,
    val updated: Int = 0,
    val failed: Int = 0,
    val error: String? = null,
)
