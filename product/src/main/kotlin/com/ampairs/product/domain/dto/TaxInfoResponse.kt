package com.ampairs.product.domain.dto

import com.ampairs.product.domain.enums.TaxSpec
import com.ampairs.product.domain.model.TaxInfo

data class TaxInfoResponse(
    var name: String = "",
    var percentage: Double = 0.0,
    var formattedName: String = "",
    var taxSpec: TaxSpec = TaxSpec.INTER,
    val active: Boolean,
    val softDeleted: Boolean,
)

fun TaxInfo.asResponse(): TaxInfoResponse {
    return TaxInfoResponse(
        name = this.name,
        formattedName = this.formattedName,
        percentage = this.percentage,
        taxSpec = this.taxSpec,
        active = this.active,
        softDeleted = this.softDeleted
    )
}

fun List<TaxInfo>.asResponse(): List<TaxInfoResponse> {
    return map {
        it.asResponse()
    }
}