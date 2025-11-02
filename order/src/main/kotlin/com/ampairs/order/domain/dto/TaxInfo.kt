package com.ampairs.order.domain.dto

data class TaxInfo(
    var id: String = "",
    var name: String = "",
    var percentage: Double = 0.0,
    var formattedName: String = "",
    var taxSpec: String = "",
    var value: Double = 0.0,
)

fun List<TaxInfo>.toInvoiceTaxInfos(): List<com.ampairs.invoice.domain.dto.TaxInfo> {
    return map {
        com.ampairs.invoice.domain.dto.TaxInfo(
            id = it.id,
            name = it.name,
            percentage = it.percentage,
            formattedName = it.formattedName,
            taxSpec = it.taxSpec,
            value = it.value
        )
    }
}