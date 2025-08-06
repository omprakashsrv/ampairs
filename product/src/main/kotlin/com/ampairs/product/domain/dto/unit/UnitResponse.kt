package com.ampairs.product.domain.dto.unit

import com.ampairs.product.domain.model.Unit

data class UnitResponse(
    var id: String, var name: String, var shortName: String, var decimalPlaces: Int, var refId: String?,
    val active: Boolean,
    val softDeleted: Boolean,
)

fun Unit.asResponse(): UnitResponse {
    return UnitResponse(
        id = this.uid, name = this.name, shortName = this.shortName, decimalPlaces = this.decimalPlaces,
        refId = this.refId,
        active = this.active,
        softDeleted = this.softDeleted
    )
}

fun List<Unit>.asResponse(): List<UnitResponse> {
    return map {
        UnitResponse(
            id = it.uid,
            name = it.name,
            shortName = it.shortName,
            decimalPlaces = it.decimalPlaces,
            refId = it.refId,
            active = it.active,
            softDeleted = it.softDeleted
        )
    }
}