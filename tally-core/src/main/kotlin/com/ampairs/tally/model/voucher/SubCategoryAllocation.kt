package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
class SubCategoryAllocation(
    @field:XmlElement(name = "SUBCATEGORY")
    var subCategory: String? = null,

    @field:XmlElement(name = "DUTYLEDGER")
    var dutyLedger: String? = null,

    @field:XmlElement(name = "TAXRATE")
    val taxRate: String? = null,

    @field:XmlElement(name = "ASSESSABLEAMOUNT")
    val assessableAmount: String? = null,

    @field:XmlElement(name = "TAX")
    val tax: String? = null,
)
