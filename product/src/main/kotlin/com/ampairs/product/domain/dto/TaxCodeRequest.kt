package com.ampairs.product.domain.dto

import com.ampairs.product.domain.enums.TaxType
import com.ampairs.product.domain.model.TaxCode
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.sql.Timestamp
import java.util.*

class TaxCodeRequest(
    val id: String,
    val code: String,
    @Enumerated(EnumType.STRING)
    var type: TaxType,
    val description: String,
    val effectiveFrom: Date?,
    val taxInfos: List<TaxInfoRequest>,
)


fun List<TaxCodeRequest>.asDatabaseModel(): List<TaxCode> {
    return map {
        val taxCode = TaxCode()
        taxCode.id = it.id
        taxCode.code = it.code
        taxCode.type = it.type
        taxCode.description = it.description
        taxCode.effectiveFrom = it.effectiveFrom?.time?.let { it1 -> Timestamp(it1) }
        taxCode.taxInfos = it.taxInfos.asDatabaseModel()
        taxCode
    }
}