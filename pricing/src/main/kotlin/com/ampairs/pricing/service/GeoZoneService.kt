package com.ampairs.pricing.service

import com.ampairs.pricing.domain.dto.GeoZoneRequest
import com.ampairs.pricing.domain.dto.GeoZoneResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Public service for the reusable geo-zone master. Owned by pricing; reused by promotions and the
 * ecom projection. [zoneForPincode] is the resolution hook used by price/offer resolvers.
 */
interface GeoZoneService {
    fun findByUid(uid: String): GeoZoneResponse?
    fun getAfterSync(lastSync: String?, pageable: Pageable): Page<GeoZoneResponse>
    fun bulkUpsert(requests: List<GeoZoneRequest>): List<GeoZoneResponse>

    /** The uid of the first active zone whose membership covers [pincode] (optionally by [state]); null if none. */
    fun zoneForPincode(pincode: String?, state: String? = null): String?
}
