package com.ampairs.invoice.domain.dto

data class TaxInfo(
    var id: String = "",
    var name: String = "",
    var percentage: Double = 0.0,
    var formattedName: String = "",
    var taxSpec: String = "",
    var value: Double = 0.0,
)