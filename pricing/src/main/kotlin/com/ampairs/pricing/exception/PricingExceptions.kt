package com.ampairs.pricing.exception

/** Thrown when a price list / item / geo-zone cannot be found for the given uid. */
class PriceListNotFoundException(message: String) : RuntimeException(message)

class GeoZoneNotFoundException(message: String) : RuntimeException(message)

/** Thrown when a price-list item's tiers are not contiguous / non-overlapping (FR-014). */
class InvalidPriceTiersException(message: String) : RuntimeException(message)
