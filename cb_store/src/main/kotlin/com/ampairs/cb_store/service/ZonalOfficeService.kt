package com.ampairs.cb_store.service

import com.ampairs.cb_store.domain.dto.ZonalOfficeRequest
import com.ampairs.cb_store.domain.dto.ZonalOfficeResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Public service surface for zonal offices. Cross-module callers depend on this interface only
 * (rule 08-module-boundaries); returns DTOs, never entities (rule 02-dto-isolation).
 */
interface ZonalOfficeService {
    fun findByUid(uid: String): ZonalOfficeResponse?
    fun getByUid(uid: String): ZonalOfficeResponse
    fun findAllActive(): List<ZonalOfficeResponse>
    fun getZonalOfficesAfterSync(lastSync: String?, pageable: Pageable): Page<ZonalOfficeResponse>
    fun bulkUpsert(requests: List<ZonalOfficeRequest>): List<ZonalOfficeResponse>
    fun create(request: ZonalOfficeRequest): ZonalOfficeResponse
    fun delete(uid: String)
}
