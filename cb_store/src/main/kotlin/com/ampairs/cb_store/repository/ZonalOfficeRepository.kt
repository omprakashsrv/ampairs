package com.ampairs.cb_store.repository

import com.ampairs.cb_store.domain.model.ZonalOffice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ZonalOfficeRepository : CrudRepository<ZonalOffice, Long> {

    fun findByUid(uid: String?): ZonalOffice?

    @EntityGraph("CbZonalOffice.basic")
    fun findByActiveTrueOrderByName(): List<ZonalOffice>

    /** Sync checkpoint: max updatedAt for the current workspace. @TenantId-filtered. */
    @Query("SELECT MAX(z.updatedAt) FROM cb_zonal_office z")
    fun findMaxUpdatedAt(): Instant?

    /** Incremental sync feed — INCLUDING inactive rows so deletions propagate. */
    @EntityGraph("CbZonalOffice.basic")
    @Query("SELECT z FROM cb_zonal_office z WHERE z.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(@Param("lastSync") lastSync: Instant, pageable: Pageable): Page<ZonalOffice>

    @EntityGraph("CbZonalOffice.basic")
    @Query("SELECT z FROM cb_zonal_office z")
    fun findAllForSync(pageable: Pageable): Page<ZonalOffice>
}
