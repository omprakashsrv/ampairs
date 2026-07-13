package com.ampairs.dms.config

/** UID prefixes for the dms (brand visibility) module. */
object Constants {
    const val SECONDARY_SALES_PREFIX = "SSS"
    const val DISTRIBUTOR_STOCK_PREFIX = "DSS"
    const val SALES_TARGET_PREFIX = "TGT"

    /** Area code used when a retailer outlet has no pincode. */
    const val UNKNOWN_AREA = "UNKNOWN"

    /** SKU bucket key for sales attributed to a brand but not yet Hop-B SKU-mapped. */
    const val UNMAPPED_SKU = "UNMAPPED"
}
