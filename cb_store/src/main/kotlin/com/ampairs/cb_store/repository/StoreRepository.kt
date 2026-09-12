package com.ampairs.cb_store.repository

import com.ampairs.cb_store.domain.model.Store
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface StoreRepository : CrudRepository<Store, Long> {

    fun findByUid(uid: String?): Store?
    fun findByOwnerIdAndCode(ownerId: String, code: String): Store?

    /** Active outlets — used by cb_maintenance's PM-generation job to roll schedules chain-wide. */
    @EntityGraph("CbStore.basic")
    fun findByActiveTrueOrderByCode(): List<Store>

    @EntityGraph("CbStore.basic")
    fun findByZonalOfficeIdAndActiveTrue(zonalOfficeId: String): List<Store>

    /** Sync checkpoint: max updatedAt for the current workspace. @TenantId-filtered. */
    @Query("SELECT MAX(s.updatedAt) FROM cb_store s")
    fun findMaxUpdatedAt(): Instant?

    /** Incremental sync feed — INCLUDING inactive rows so deletions propagate. */
    @EntityGraph("CbStore.basic")
    @Query("SELECT s FROM cb_store s WHERE s.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(@Param("lastSync") lastSync: Instant, pageable: Pageable): Page<Store>

    @EntityGraph("CbStore.basic")
    @Query("SELECT s FROM cb_store s")
    fun findAllForSync(pageable: Pageable): Page<Store>
}
