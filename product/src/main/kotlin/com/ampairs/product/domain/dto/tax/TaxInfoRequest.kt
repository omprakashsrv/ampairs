package com.ampairs.product.domain.dto.tax

import com.ampairs.product.domain.enums.TaxSpec
import com.ampairs.product.domain.model.TaxInfo

data class TaxInfoRequest(
    var id: String? = "",
    var refId: String?,
    var name: String = "",
    var percentage: Double = 0.0,
    var formattedName: String? = "",
    var taxSpec: TaxSpec? = TaxSpec.INTER,
    val active: Boolean,
    val softDeleted: Boolean,
)


fun List<TaxInfoRequest>.asDatabaseModel(): List<TaxInfo> {
    return map {
        it.asDatabaseModel()
    }
}

fun TaxInfoRequest.asDatabaseModel(): TaxInfo {
    val taxInfo = TaxInfo()
    taxInfo.id = this.id ?: ""
    taxInfo.refId = this.refId
    taxInfo.name = this.name
    taxInfo.formattedName = this.formattedName ?: this.name
    taxInfo.percentage = this.percentage
    taxInfo.taxSpec = this.taxSpec ?: TaxSpec.INTRA
    taxInfo.active = this.active
    taxInfo.softDeleted = this.softDeleted
    return taxInfo
}