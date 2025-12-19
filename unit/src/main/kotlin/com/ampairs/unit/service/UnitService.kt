package com.ampairs.unit.service

import com.ampairs.unit.domain.dto.UnitRequest
import com.ampairs.unit.domain.dto.UnitResponse
import com.ampairs.unit.domain.dto.UnitUsageResponse

interface UnitService {
    fun findByUid(uid: String): UnitResponse?
    fun findByRefId(refId: String): UnitResponse?
    fun findAll(activeOnly: Boolean = true): List<UnitResponse>
    fun create(request: UnitRequest): UnitResponse
    fun update(uid: String, request: UnitRequest): UnitResponse
    fun delete(uid: String)
    fun isUnitInUse(uid: String): Boolean
    fun findEntitiesUsingUnit(uid: String): List<String>
    fun getUsage(uid: String): UnitUsageResponse
}
