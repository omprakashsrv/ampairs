package com.ampairs.event.service

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.sync.SyncCheckpointContributor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Aggregates per-entity sync checkpoints (`max(updatedAt)` per syncable entity type) across all
 * domain modules for the current workspace.
 *
 * Each module registers a [SyncCheckpointContributor] bean; Spring injects all of them here. The
 * checkpoints are derived directly from each entity's `updatedAt` column (never null once a row
 * exists), so this covers all pre-existing data with no seeding or migration.
 *
 * Source of truth for the mobile client's connect/reconnect/hourly bootstrap.
 */
@Service
class SyncCheckpointService(
    private val contributors: List<SyncCheckpointContributor>
) {

    private val logger = LoggerFactory.getLogger(SyncCheckpointService::class.java)

    /**
     * Merge every contributor's checkpoints for the current workspace.
     *
     * A given entity code maps to `null` when it has no rows. If two contributors report the same
     * code, the later timestamp wins. A failing contributor is logged and skipped — it must not
     * take down the whole checkpoint response.
     */
    fun checkpoints(): Map<String, Instant?> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context")

        val merged = LinkedHashMap<String, Instant?>()
        contributors.forEach { contributor ->
            val contribution = try {
                contributor.checkpoints()
            } catch (e: Exception) {
                logger.error("Checkpoint contributor {} failed", contributor::class.simpleName, e)
                emptyMap()
            }
            contribution.forEach { (code, timestamp) ->
                val existing = merged[code]
                merged[code] = when {
                    timestamp == null -> existing
                    existing == null -> timestamp
                    else -> if (timestamp.isAfter(existing)) timestamp else existing
                }
            }
        }

        logger.debug("Resolved {} entity checkpoints for workspace {}", merged.size, workspaceId)
        return merged
    }
}
