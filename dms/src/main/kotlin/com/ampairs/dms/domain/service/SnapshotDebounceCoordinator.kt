package com.ampairs.dms.domain.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Coalesces secondary-sales / stock snapshot rebuilds to **at most once per ~5 minutes per
 * distributor** (FR-022 / SC-011). A qualifying source change (invoice/order/inventory event) calls
 * [shouldRebuild]; it returns true only when the per-distributor window has elapsed, recording the
 * rebuild time. Time is injected (`nowMillis`) so the coalescing is deterministically unit-testable.
 *
 * This is the trigger gate only — the actual recompute (building RawSales from the distributor's
 * source documents and calling SnapshotService) is wired by the event listener that owns the gate.
 */
@Component
class SnapshotDebounceCoordinator(
    @Value("\${dms.snapshot.coalesce-window-millis:300000}")
    private val windowMillis: Long = DEFAULT_WINDOW_MILLIS,
) {
    private val lastRebuildAt = ConcurrentHashMap<String, Long>()

    /**
     * @return true if a rebuild should run now for [distributorWorkspaceId] (and records [nowMillis]);
     *         false if a rebuild already ran within the coalescing window.
     */
    fun shouldRebuild(distributorWorkspaceId: String, nowMillis: Long): Boolean {
        val previous = lastRebuildAt[distributorWorkspaceId]
        return if (previous == null || nowMillis - previous >= windowMillis) {
            lastRebuildAt[distributorWorkspaceId] = nowMillis
            true
        } else {
            false
        }
    }

    companion object {
        const val DEFAULT_WINDOW_MILLIS = 5L * 60L * 1000L // ~5 minutes
    }
}
