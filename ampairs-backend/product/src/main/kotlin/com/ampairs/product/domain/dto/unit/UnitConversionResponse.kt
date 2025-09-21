package com.ampairs.product.domain.dto.unit

import com.ampairs.product.domain.model.UnitConversion

data class UnitConversionResponse(
    var id: String,
    var baseUnit: UnitResponse,
    var derivedUnit: UnitResponse,
    var multiplier: Double,
)


fun List<UnitConversion>.asUnitConversionResponse(): List<UnitConversionResponse> {
    return map {
        UnitConversionResponse(
            id = it.uid,
            baseUnit = it.baseUnit.asResponse(),
            derivedUnit = it.derivedUnit.asResponse(),
            multiplier = it.multiplier,
        )
    }
}