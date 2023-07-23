package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.Unit

data class UnitRequest(
    var name: String, var shortName: String, var decimalPlaces: Int
)

fun List<UnitRequest>.asDatabaseModel(): List<Unit> {
    return map {
        val unit = Unit()
        unit.name = it.name
        unit.shortName = it.shortName
        unit.decimalPlaces = it.decimalPlaces
        unit
    }
}