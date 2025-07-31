package com.ampairs.product.domain.dto.unit

import com.ampairs.product.domain.model.UnitConversion

data class UnitConversionRequest(
    var id: String,
    var refId: String,
    var baseUnit: String,
    var derivedUnit: String,
    var multiplier: Double
)


fun List<UnitConversionRequest>.asDatabaseModel(): List<UnitConversion> {
    return map {
        val unitConversion = UnitConversion()
        unitConversion.seqId = it.id
        unitConversion.baseUnitId = it.baseUnit
        unitConversion.derivedUnitId = it.derivedUnit
        unitConversion.multiplier = it.multiplier
        unitConversion
    }
}