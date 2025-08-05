package com.ampairs.product.ui.tax.tax_info

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ampairs.product.domain.TaxInfo
import com.ampairs.product.domain.TaxSpec

class TaxInfoState(val taxInfo: TaxInfo) {
    var id by mutableStateOf(this.taxInfo.id)
    var name by mutableStateOf(this.taxInfo.name)
    var percentage by mutableStateOf(this.taxInfo.percentage.toString())
    var formattedName by mutableStateOf(this.taxInfo.formattedName)
    var taxSpec by mutableStateOf(this.taxInfo.taxSpec)
    var taxSpecExpanded by mutableStateOf(false)
    val taxSpecs = TaxSpec.entries.toTypedArray()
    var selectedSpecIndex by mutableStateOf(taxSpecs.indexOf(this.taxInfo.taxSpec))
}

fun TaxInfoState.toDomainModel(): TaxInfo {
    this.taxInfo.id = id
    this.taxInfo.name = name
    this.taxInfo.percentage = percentage.toDouble()
    this.taxInfo.formattedName = formattedName
    this.taxInfo.taxSpec = taxSpec
    return this.taxInfo
}