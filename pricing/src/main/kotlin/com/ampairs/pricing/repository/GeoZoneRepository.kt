package com.ampairs.pricing.repository

import com.ampairs.pricing.domain.model.GeoZone
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface GeoZoneRepository : CrudRepository<GeoZone, Long> {

    fun findByUid(uid: String?): GeoZone?

    fun findAllByActiveTrue(): List<GeoZone>

    // ── sync feed (includes inactive rows) ────────────────────────────────────────────
    @Query("SELECT z FROM geo_zone z WHERE z.updatedAt > :updatedAt ORDER BY z.updatedAt ASC")
    fun findByUpdatedAtAfter(@Param("updatedAt") updatedAt: Instant, pageable: Pageable): Page<GeoZone>

    @Query("SELECT z FROM geo_zone z")
    fun findAllForSync(pageable: Pageable): Page<GeoZone>

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(z.updatedAt) FROM geo_zone z")
    fun findMaxUpdatedAt(): Instant?
}
