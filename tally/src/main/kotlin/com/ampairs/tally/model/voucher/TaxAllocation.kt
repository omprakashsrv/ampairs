package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
class TaxAllocation {
    //        <ISOPTIONAL>No</ISOPTIONAL>
    //        <ZERORATED>No</ZERORATED>
    //        <EXEMPTED>No</EXEMPTED>
    //        <ISSPECIALRATE>No</ISSPECIALRATE>
    //        <ISDEDUCTNOW>No</ISDEDUCTNOW>
    //        <ISSUPPLEMENTARY>No</ISSUPPLEMENTARY>
    //        <ISPUREAGENT>No</ISPUREAGENT>
    //        <HASINPUTCREDIT>No</HASINPUTCREDIT>
    @XmlElement(name = "CATEGORY")
    var category: String? = null

    @XmlElement(name = "TAXTYPE")
    var taxType: String? = null

    @XmlElement(name = "TAXNAME")
    var taxName: String? = null

    @XmlElement(name = "PARTYLEDGER")
    var partyLedger: String? = null

    @XmlElement(name = "EXPENSES")
    var expenses: String? = null

    @XmlElement(name = "REFTYPE")
    var refType: String? = null

    @XmlElement(name = "ISPANVALID")
    var isPanValid: String? = null

    @XmlElement(name = "ISPANNOTAVAILABLE")
    var isPanNotAvailable: String? = null

    @XmlElement(name = "SUBCATEGORYALLOCATION.LIST", type = SubCategoryAllocation::class)
    var subCategoryAllocation: List<SubCategoryAllocation>? = null
}
