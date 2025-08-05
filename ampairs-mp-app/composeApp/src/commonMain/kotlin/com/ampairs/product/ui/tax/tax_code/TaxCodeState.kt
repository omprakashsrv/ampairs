package com.ampairs.product.ui.tax.tax_code

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ampairs.product.domain.TaxCode
import com.ampairs.product.domain.TaxType

class TaxCodeState(var taxCode: TaxCode) {
    var id by mutableStateOf(this.taxCode.id)
    var code by mutableStateOf(this.taxCode.code)
    var type by mutableStateOf(this.taxCode.type)
    var description by mutableStateOf(this.taxCode.description)
    var effectiveFrom by mutableStateOf(this.taxCode.effectiveFrom)
    var taxInfos by mutableStateOf(this.taxCode.taxInfos)

    var taxTypeExpanded by mutableStateOf(false)
    val taxTypes = TaxType.entries.toTypedArray()
    var selectedTypeIndex by mutableStateOf(taxTypes.indexOf(this.taxCode.type))
    var taxInfoExpanded by mutableStateOf(false)
    var showDatePicker by mutableStateOf(false)
}

fun TaxCodeState.toDomainModel(): TaxCode {
    this.taxCode.id = this.id
    this.taxCode.code = this.code
    this.taxCode.type = this.type
    this.taxCode.description = this.description
    this.taxCode.effectiveFrom = this.effectiveFrom
    this.taxCode.taxInfos = this.taxInfos
    return this.taxCode
}