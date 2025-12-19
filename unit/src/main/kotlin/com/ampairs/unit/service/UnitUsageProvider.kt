package com.ampairs.unit.service

/**
 * Extension point allowing other modules (product, inventory, invoice, etc.)
 * to provide unit usage metadata without creating direct compile-time dependencies.
 */
fun interface UnitUsageProvider {
    fun findUsage(unitUid: String): UnitUsageSnapshot
}

data class UnitUsageSnapshot(
    val unitUid: String,
    val entityIds: List<String> = emptyList(),
    val conversionIds: List<String> = emptyList()
) {
    val entityCount: Int = entityIds.size
    val conversionCount: Int = conversionIds.size
    val inUse: Boolean = entityIds.isNotEmpty() || conversionIds.isNotEmpty()

    companion object {
        val EMPTY = UnitUsageSnapshot(unitUid = "", entityIds = emptyList(), conversionIds = emptyList())
    }
}

/**
 * Utility merge function to aggregate usage snapshots from multiple providers.
 */
fun Collection<UnitUsageSnapshot>.merge(unitUid: String): UnitUsageSnapshot {
    if (isEmpty()) return UnitUsageSnapshot(unitUid = unitUid)
    val entityIds = mutableSetOf<String>()
    val conversionIds = mutableSetOf<String>()
    forEach {
        entityIds.addAll(it.entityIds)
        conversionIds.addAll(it.conversionIds)
    }
    return UnitUsageSnapshot(
        unitUid = unitUid,
        entityIds = entityIds.toList(),
        conversionIds = conversionIds.toList()
    )
}
