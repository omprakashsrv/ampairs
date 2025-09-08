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
    var componentType: TaxComponentType? = TaxComponentType.IGST,
    var isCompoundTax: Boolean = false,
    var baseAmount: Double? = null,
    var calculatedAmount: Double? = null
)

/**
 * GST component types for Indian taxation
 */
enum class TaxComponentType(val displayName: String, val description: String) {
    CGST("Central GST", "Central Goods and Services Tax"),
    SGST("State GST", "State Goods and Services Tax"), 
    IGST("Integrated GST", "Integrated Goods and Services Tax"),
    UTGST("Union Territory GST", "Union Territory Goods and Services Tax"),
    CESS("Cess", "Additional cess on specific goods"),
    TDS("Tax Deducted at Source", "TDS applicable"),
    TCS("Tax Collected at Source", "TCS applicable")
}

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