package com.ampairs.cb_store.service

import com.ampairs.cb_store.domain.dto.StoreRequest
import com.ampairs.cb_store.domain.dto.StoreResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Public service surface for outlets. Cross-module callers (notably `cb_maintenance`) depend on
 * this interface only. Returns DTOs, never entities.
 */
interface StoreService {
    fun findByUid(uid: String): StoreResponse?
    fun getByUid(uid: String): StoreResponse

    /** Denormalization helper: the zonal office a store belongs to (drives ticket/PM access scoping). */
    fun getZonalOfficeId(storeId: String): String

    /** Active outlets — cb_maintenance iterates these to roll PM schedules chain-wide. */
    fun findAllActive(): List<StoreResponse>

    fun getStoresAfterSync(lastSync: String?, pageable: Pageable): Page<StoreResponse>
    fun bulkUpsert(requests: List<StoreRequest>): List<StoreResponse>
    fun create(request: StoreRequest): StoreResponse
    fun delete(uid: String)
}
