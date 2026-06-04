package com.ampairs.core.sync

import java.time.Instant

/**
 * SPI implemented by each domain module to contribute its per-entity sync checkpoints —
 * the `max(updatedAt)` per syncable entity type — for the current workspace.
 *
 * Implementations rely on the ambient tenant context (set upstream per request): the underlying
 * `@TenantId`-filtered queries are automatically scoped to the current workspace, so no
 * `workspaceId` parameter is needed.
 *
 * The event module collects every contributor at runtime (`List<SyncCheckpointContributor>`) and
 * merges them into a single checkpoint map. This keeps module boundaries intact — the event module
 * never depends on domain modules at compile time; contributions flow in via this core interface.
 *
 * Used by the mobile client's connect/reconnect/hourly bootstrap: for each entity it compares the
 * returned `updatedAt` against its own last-synced timestamp and pulls only the lagging entities.
 */
interface SyncCheckpointContributor {

    /**
     * @return map of mobile `SyncEntity` code (e.g. "customer", "customer_group", "product") to the
     *         `max(updatedAt)` of that entity for the current workspace. A `null` value means
     *         "no rows for that entity" — the client must not pull it.
     */
    fun checkpoints(): Map<String, Instant?>
}
