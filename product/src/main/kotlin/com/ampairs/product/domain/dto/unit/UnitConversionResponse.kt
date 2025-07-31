package com.ampairs.product.domain.dto.unit

import com.ampairs.product.domain.model.UnitConversion

data class UnitConversionResponse(
    var id: String,
    var baseUnit: UnitResponse,
    var derivedUnit: UnitResponse,
    var multiplier: Double,
    val active: Boolean,
    val softDeleted: Boolean,
)


fun List<UnitConversion>.asUnitConversionResponse(): List<UnitConversionResponse> {
    return map {
        UnitConversionResponse(
            id = it.seqId,
            baseUnit = it.baseUnit.asResponse(),
            derivedUnit = it.derivedUnit.asResponse(),
            multiplier = it.multiplier,
            active = it.active,
            softDeleted = it.softDeleted
        )
    }
}