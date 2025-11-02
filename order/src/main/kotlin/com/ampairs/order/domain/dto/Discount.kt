package com.ampairs.order.domain.dto

data class Discount(
    var percent: Double,
    var value: Double,
)

fun List<Discount>.toInvoiceDiscount(): List<com.ampairs.invoice.domain.dto.Discount> {
    return map {
        com.ampairs.invoice.domain.dto.Discount(
            percent = it.percent,
            value = it.value
        )
    }
}