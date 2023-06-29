package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.TaxCode
import com.ampairs.product.domain.model.TaxType
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.util.*

data class TaxCodeResponse(
    val id: String,
    val code: String,
    @Enumerated(EnumType.STRING) var type: TaxType,
    val description: String,
    val effectiveFrom: Date?,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val cess: Double
)


fun List<TaxCode>.asTaxCodeResponse(): List<TaxCodeResponse> {
    return map {
        TaxCodeResponse(
            id = it.id,
            code = it.code,
            description = it.description,
            type = it.type,
            cgst = it.cgst,
            sgst = it.sgst,
            igst = it.igst,
            cess = it.cess,
            effectiveFrom = it.effectiveFrom?.time?.let { it1 -> Date(it1) }
        )
    }
}