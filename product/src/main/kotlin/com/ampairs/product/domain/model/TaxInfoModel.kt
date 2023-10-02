package com.ampairs.product.domain.model

import com.ampairs.product.domain.dto.tax.TaxInfoRequest
import com.ampairs.product.domain.dto.tax.TaxInfoResponse
import com.ampairs.product.domain.enums.TaxSpec

data class TaxInfoModel(
    var id: String? = "",
    var refId: String?,
    var name: String = "",
    var percentage: Double = 0.0,
    var formattedName: String? = "",
    var taxSpec: TaxSpec? = TaxSpec.INTER,
)

fun List<TaxInfoRequest>.asDomainModel(): List<TaxInfoModel> {
    return map {
        TaxInfoModel(
            id = it.id,
            refId = it.refId,
            name = it.name,
            percentage = it.percentage,
            formattedName = it.formattedName,
            taxSpec = it.taxSpec
        )
    }
}

fun List<TaxInfoModel>.asResponse(): List<TaxInfoResponse> {
    return map {
        TaxInfoResponse(
            id = it.id ?: "",
            name = it.name,
            percentage = it.percentage,
            formattedName = it.formattedName ?: "",
            taxSpec = it.taxSpec ?: TaxSpec.INTER,
            active = true,
            softDeleted = false
        )
    }
}