package com.ampairs.product.domain.dto.unit

import com.ampairs.product.domain.model.Unit

data class UnitRequest(
    var id: String,
    var name: String,
    var shortName: String?,
    var decimalPlaces: Int,
    var refId: String?
)

fun List<UnitRequest>.asDatabaseModel(): List<Unit> {
    return map {
        val unit = Unit()
        unit.uid = it.id
        unit.name = it.name
        unit.shortName = it.shortName ?: it.name
        unit.decimalPlaces = it.decimalPlaces
        unit.refId = it.refId
        unit
    }
}