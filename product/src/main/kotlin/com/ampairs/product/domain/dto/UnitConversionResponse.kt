package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.UnitConversion

data class UnitConversionResponse(
    var id: String,
    var baseUnit: UnitResponse,
    var derivedUnit: UnitResponse,
    var multiplier: Double
)


fun List<UnitConversion>.asUnitConversionResponse(): List<UnitConversionResponse> {
    return map {
        com.ampairs.product.domain.dto.UnitConversionResponse(
            id = it.id,
            baseUnit = it.baseUnit.asResponse(),
            derivedUnit = it.derivedUnit.asResponse(),
            multiplier = it.multiplier
        )
    }
}