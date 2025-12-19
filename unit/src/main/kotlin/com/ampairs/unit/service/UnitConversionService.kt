package com.ampairs.unit.service

import com.ampairs.unit.domain.dto.UnitConversionRequest
import com.ampairs.unit.domain.dto.UnitConversionResponse

interface UnitConversionService {
    fun findByUid(uid: String): UnitConversionResponse?
    fun findByEntityId(entityId: String): List<UnitConversionResponse>
    fun findAll(): List<UnitConversionResponse>
    fun convert(quantity: Double, fromUnitId: String, toUnitId: String, entityId: String? = null): Double
    fun create(request: UnitConversionRequest): UnitConversionResponse
    fun update(uid: String, request: UnitConversionRequest): UnitConversionResponse
    fun delete(uid: String)
    fun validateNoCircularConversion(baseUnitId: String, derivedUnitId: String, entityId: String? = null)
}
