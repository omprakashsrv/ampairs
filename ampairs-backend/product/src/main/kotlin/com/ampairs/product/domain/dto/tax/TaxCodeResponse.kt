package com.ampairs.product.domain.dto.tax

import com.ampairs.product.domain.enums.TaxType
import com.ampairs.product.domain.model.TaxCode
import com.ampairs.product.domain.model.asResponse
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.util.*

data class TaxCodeResponse(
    val id: String,
    val code: String,
    @Enumerated(EnumType.STRING) var type: TaxType,
    val description: String,
    val effectiveFrom: Date?,
    val taxInfos: List<TaxInfoResponse>,
    val active: Boolean,
    val softDeleted: Boolean,
)


fun List<TaxCode>.asResponse(): List<TaxCodeResponse> {
    return map {
        TaxCodeResponse(
            id = it.uid,
            code = it.code,
            description = it.description,
            type = it.type,
            effectiveFrom = it.effectiveFrom?.time?.let { it1 -> Date(it1) },
            taxInfos = it.taxInfos.asResponse(),
            active = it.active,
            softDeleted = it.softDeleted
        )
    }
}