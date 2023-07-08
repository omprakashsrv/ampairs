package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class TaxAllocation(
    @field:XmlElement(name = "CATEGORY")
    var category: String? = null,

    @field:XmlElement(name = "TAXTYPE")
    var taxType: String? = null,

    @field:XmlElement(name = "TAXNAME")
    var taxName: String? = null,

    @field:XmlElement(name = "PARTYLEDGER")
    var partyLedger: String? = null,

    @field:XmlElement(name = "EXPENSES")
    var expenses: String? = null,

    @field:XmlElement(name = "REFTYPE")
    var refType: String? = null,

    @field:XmlElement(name = "ISPANVALID")
    var isPanValid: String? = null,

    @field:XmlElement(name = "ISPANNOTAVAILABLE")
    var isPanNotAvailable: String? = null,

    @field:XmlElement(name = "SUBCATEGORYALLOCATION.LIST", type = SubCategoryAllocation::class)
    var subCategoryAllocation: List<SubCategoryAllocation>? = null,
)
