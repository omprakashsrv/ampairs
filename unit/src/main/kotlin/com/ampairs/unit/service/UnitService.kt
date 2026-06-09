package com.ampairs.unit.service

import com.ampairs.unit.domain.dto.UnitRequest
import com.ampairs.unit.domain.dto.UnitResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UnitService {
    fun findByUid(uid: String): UnitResponse?
    fun findByRefId(refId: String): UnitResponse?
    fun findAll(activeOnly: Boolean = true): List<UnitResponse>
    fun findAllPaged(activeOnly: Boolean, pageable: Pageable): Page<UnitResponse>

    /**
     * Incremental sync feed — units updated at/after lastSync, INCLUDING inactive
     * (soft-deleted) rows so clients can detect deletions. Blank/null lastSync returns
     * all rows for the workspace (paginated), still including inactive.
     */
    fun getUnitsAfterSync(lastSync: String?, pageable: Pageable): Page<UnitResponse>

    /**
     * Bulk upsert units keyed by uid — create when uid absent/unknown, update when present.
     * Honors the active flag so soft-deletes propagate in-band.
     */
    fun bulkUpsert(requests: List<UnitRequest>): List<UnitResponse>

    fun create(request: UnitRequest): UnitResponse
    fun delete(uid: String)
    fun isUnitInUse(uid: String): Boolean
    fun findEntitiesUsingUnit(uid: String): List<String>
}
