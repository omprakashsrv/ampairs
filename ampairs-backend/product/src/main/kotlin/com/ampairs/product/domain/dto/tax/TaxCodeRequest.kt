package com.ampairs.product.domain.dto.tax

import com.ampairs.product.domain.enums.TaxType
import com.ampairs.product.domain.model.TaxCode
import com.ampairs.product.domain.model.asDomainModel
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.sql.Timestamp
import java.util.*

class TaxCodeRequest(
    val id: String,
    val refId: String?,
    val code: String,
    @Enumerated(EnumType.STRING)
    var type: TaxType,
    val description: String,
    val effectiveFrom: Date?,
    val taxInfos: List<TaxInfoRequest>?,
    val active: Boolean,
    val softDeleted: Boolean,
)


fun List<TaxCodeRequest>.asDatabaseModel(): List<TaxCode> {
    return map {
        val taxCode = TaxCode()
        taxCode.uid = it.id
        taxCode.refId = it.refId
        taxCode.code = it.code
        taxCode.type = it.type
        taxCode.description = it.description
        taxCode.effectiveFrom = it.effectiveFrom?.time?.let { it1 -> Timestamp(it1) }
        taxCode.taxInfos = it.taxInfos?.asDomainModel() ?: arrayListOf()
        taxCode.active = it.active
        taxCode.softDeleted = it.softDeleted
        taxCode
    }
}