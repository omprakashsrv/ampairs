package com.ampairs.pricing.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.pricing.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * Reusable named geography zone (set of pincodes / ranges / states). Referenced by `PriceList.geoZoneId`
 * and by promotions; the resolver maps a customer/delivery pincode → zone. Owned by the pricing module.
 */
@Entity(name = "geo_zone")
@Table(
    indexes = [
        Index(name = "idx_geo_zone_uid", columnList = "uid", unique = true),
        Index(name = "idx_geo_zone_owner", columnList = "owner_id"),
    ]
)
class GeoZone : OwnableBaseDomain() {

    @Column(name = "name", length = 200, nullable = false)
    var name: String = ""

    /** JSON [GeoZoneMembers] — pincodes, pincode-ranges, states. */
    @Column(name = "members_json", columnDefinition = "TEXT")
    var membersJson: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.GEO_ZONE_PREFIX
}
