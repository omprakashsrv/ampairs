package com.ampairs.dms.domain.enums

/** Aggregation grain of a secondary-sales snapshot. */
enum class SnapshotGrain {
    SKU_PERIOD,
    AREA_PERIOD,
}

/** Tier a sales target applies to. */
enum class TargetTier {
    PRIMARY,    // brand → distributor
    SECONDARY,  // distributor / rep → beat
}
