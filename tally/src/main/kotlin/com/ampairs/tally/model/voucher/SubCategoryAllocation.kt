package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
class SubCategoryAllocation {
    @XmlElement(name = "SUBCATEGORY")
    var subCategory: String? = null

    @XmlElement(name = "DUTYLEDGER")
    var dutyLedger: String? = null

    @XmlElement(name = "TAXRATE")
    val taxRate: String? = null

    @XmlElement(name = "ASSESSABLEAMOUNT")
    val assessableAmount: String? = null

    @XmlElement(name = "TAX")
    val tax: String? = null
}
