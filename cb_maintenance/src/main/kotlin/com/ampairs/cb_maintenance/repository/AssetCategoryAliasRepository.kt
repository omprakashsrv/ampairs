package com.ampairs.cb_maintenance.repository

import com.ampairs.cb_maintenance.domain.model.AssetCategoryAlias
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface AssetCategoryAliasRepository : CrudRepository<AssetCategoryAlias, Long> {

    fun findByUid(uid: String?): AssetCategoryAlias?

    /** Canonicalization lookup — resolves a messy source name to its canonical asset category. */
    fun findByAliasLowerAndActiveTrue(aliasLower: String): AssetCategoryAlias?

    @Query("SELECT MAX(a.updatedAt) FROM cb_asset_category_alias a")
    fun findMaxUpdatedAt(): Instant?

    @EntityGraph("CbAssetCategoryAlias.basic")
    @Query("SELECT a FROM cb_asset_category_alias a")
    fun findAllForSync(pageable: Pageable): Page<AssetCategoryAlias>

    @EntityGraph("CbAssetCategoryAlias.basic")
    @Query("SELECT a FROM cb_asset_category_alias a WHERE a.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(@Param("lastSync") lastSync: Instant, pageable: Pageable): Page<AssetCategoryAlias>
}
