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
    val productIds: List<String> = emptyList(),
    val conversionIds: List<String> = emptyList()
) {
    val productCount: Int = productIds.size
    val conversionCount: Int = conversionIds.size
    val inUse: Boolean = productIds.isNotEmpty() || conversionIds.isNotEmpty()

    companion object {
        val EMPTY = UnitUsageSnapshot(unitUid = "", productIds = emptyList(), conversionIds = emptyList())
    }
}

/**
 * Utility merge function to aggregate usage snapshots from multiple providers.
 */
fun Collection<UnitUsageSnapshot>.merge(unitUid: String): UnitUsageSnapshot {
    if (isEmpty()) return UnitUsageSnapshot(unitUid = unitUid)
    val productIds = mutableSetOf<String>()
    val conversionIds = mutableSetOf<String>()
    forEach {
        productIds.addAll(it.productIds)
        conversionIds.addAll(it.conversionIds)
    }
    return UnitUsageSnapshot(
        unitUid = unitUid,
        productIds = productIds.toList(),
        conversionIds = conversionIds.toList()
    )
}
