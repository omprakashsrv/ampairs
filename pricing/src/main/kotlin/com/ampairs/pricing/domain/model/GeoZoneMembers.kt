package com.ampairs.pricing.domain.model

/**
 * Membership of a [GeoZone] — the set of pincodes/ranges/states the zone covers.
 * Stored as JSON on `geo_zone.members_json`. An exact-pincode zone is just one entry in [pincodes].
 */
data class GeoZoneMembers(
    val pincodes: List<String> = emptyList(),
    val pincodeRanges: List<PincodeRange> = emptyList(),
    val states: List<String> = emptyList(),
) {
    data class PincodeRange(val from: String, val to: String)

    /** True if [pincode] falls in this zone (exact list, a numeric range, or — when given — its [state]). */
    fun contains(pincode: String, state: String? = null): Boolean {
        if (pincode in pincodes) return true
        if (state != null && states.any { it.equals(state, ignoreCase = true) }) return true
        val numeric = pincode.toLongOrNull()
        if (numeric != null && pincodeRanges.any { r ->
                val from = r.from.toLongOrNull()
                val to = r.to.toLongOrNull()
                from != null && to != null && numeric in from..to
            }
        ) return true
        return false
    }
}
