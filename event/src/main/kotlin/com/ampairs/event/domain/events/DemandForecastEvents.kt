package com.ampairs.event.domain.events

/**
 * Published by the `analytics` module after demand forecasts are (re)computed for a workspace
 * (R6 / FR-018). Replenishment (feature 027) and inventory consume this to refresh reorder
 * point / safety stock — they then pull per-product figures from analytics' `DemandSignalService`.
 * Analytics never writes inventory/replenishment tables; it only signals.
 *
 * Lives in the `event` module so consumers depend on `event`, not on `analytics`.
 */
class DemandForecastUpdatedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val productCount: Int,
    val generatedAtEpochMillis: Long,
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> = mapOf(
        "action" to "demand_forecast_updated",
        "productCount" to productCount,
        "generatedAtEpochMillis" to generatedAtEpochMillis,
    )
}
