package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.TaxCode
import com.ampairs.product.domain.model.TaxType
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.sql.Timestamp
import java.util.*

class TaxCodeRequest(
    val id: String,
    val refId: String,
    val code: String,
    @Enumerated(EnumType.STRING)
    var type: TaxType,
    val description: String,
    val effectiveFrom: Date?,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val cess: Double
)


fun List<TaxCodeRequest>.asDatabaseModel(): List<TaxCode> {
    return map {
        val taxCode = TaxCode()
        taxCode.id = it.id
        taxCode.refId = it.refId
        taxCode.code = it.code
        taxCode.type = it.type
        taxCode.description = it.description
        taxCode.effectiveFrom = it.effectiveFrom?.time?.let { it1 -> Timestamp(it1) }
        taxCode.cgst = it.cgst
        taxCode.sgst = it.sgst
        taxCode.igst = it.igst
        taxCode.cess = it.cess
        taxCode
    }
}