package com.ampairs.setting.repository

import com.ampairs.setting.domain.model.StoreSetting
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface StoreSettingRepository : CrudRepository<StoreSetting, Long> {

    fun findByUid(uid: String?): StoreSetting?

    fun findByModuleCodeAndSettingKey(moduleCode: String, settingKey: String): StoreSetting?

    /** Incremental pull: rows changed after [updatedAt] (includes inactive so deletes propagate). */
    @EntityGraph("StoreSetting.basic")
    @Query("SELECT s FROM store_setting s WHERE s.updatedAt > :updatedAt ORDER BY s.updatedAt ASC")
    fun findByUpdatedAtAfter(@Param("updatedAt") updatedAt: Instant, pageable: Pageable): Page<StoreSetting>

    @EntityGraph("StoreSetting.basic")
    @Query("SELECT s FROM store_setting s ORDER BY s.updatedAt ASC")
    fun findAllOrderByUpdatedAt(pageable: Pageable): Page<StoreSetting>

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(s.updatedAt) FROM store_setting s")
    fun findMaxUpdatedAt(): Instant?
}
