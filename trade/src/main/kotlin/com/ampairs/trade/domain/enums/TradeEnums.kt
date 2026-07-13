package com.ampairs.trade.domain.enums

/** Lifecycle of a brand↔distributor trade link. Data flows only while ACCEPTED. */
enum class LinkStatus {
    INVITED,
    ACCEPTED,
    DECLINED,
    REVOKED,
}

/** How much of a retailer outlet a brand may see over a link. Full contact PII never crosses. */
enum class RetailerVisibility {
    CODED,
    IDENTIFIED,
}

/** Category of data a consent scope may permit a brand to read. */
enum class DataCategory {
    SECONDARY_SALES,
    STOCK,
    TARGETS,
}

/** Whether a distributor brand-label designation (Hop A) is active. */
enum class DesignationStatus {
    ACTIVE,
    REMOVED,
}

/** How a Hop-B SKU mapping was proposed (never HSN). */
enum class MatchSource {
    AUTO_BARCODE,
    AUTO_SKU,
    MANUAL,
}

/** Confirmation state of a Hop-B distributor↔brand-SKU mapping. */
enum class MappingStatus {
    SUGGESTED,
    CONFIRMED,
}

/** Publication state of a pricing/015 scheme down a link. */
enum class PublicationStatus {
    PUBLISHED,
    WITHDRAWN,
}

/** Brand→distributor primary-order handshake state. */
enum class PrimaryOrderStatus {
    PLACED,
    CONFIRMED,
    REJECTED,
}

/** Tier of a participant in the trade network. */
enum class TradeTier {
    BRAND,
    DISTRIBUTOR,
    RETAILER,
}
