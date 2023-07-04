package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.Unit

data class UnitResponse(
    var id: String, var name: String, var shortName: String, var decimalPlaces: Int
)

fun Unit.asUnitResponse(): UnitResponse {
    return UnitResponse(
        id = this.id, name = this.name, shortName = this.shortName, decimalPlaces = this.decimalPlaces
    )
}