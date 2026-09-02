package com.ampairs.cb_maintenance.domain.model

import com.ampairs.cb_maintenance.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * Normalizes the messy source names for an asset category (`AC` / `A/C Plant` / `Aircon Unit` all →
 * one canonical value). Owned by cb_maintenance — only ever used to resolve PmSchedule/Ticket
 * assetCategory (module plan §3.1).
 */
@Entity(name = "cb_asset_category_alias")
@NamedEntityGraph(name = "CbAssetCategoryAlias.basic")
@Table(
    name = "asset_category_alias",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_cb_asset_alias_owner_alias", columnNames = ["owner_id", "alias_lower"]),
    ],
    indexes = [
        Index(name = "idx_cb_asset_alias_uid", columnList = "uid", unique = true),
        Index(name = "idx_cb_asset_alias_owner", columnList = "owner_id"),
    ]
)
class AssetCategoryAlias : OwnableBaseDomain() {

    @Column(name = "canonical", length = 100, nullable = false)
    var canonical: String = ""

    @Column(name = "alias", length = 100, nullable = false)
    var alias: String = ""

    /** Lower-cased alias, maintained by the service, for the case-insensitive uniqueness constraint. */
    @Column(name = "alias_lower", length = 100, nullable = false)
    var aliasLower: String = ""

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return Constants.ASSET_CATEGORY_ALIAS_PREFIX
    }
}
