package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class BasicRateOfInvoiceTax(
    @field:XmlElement(name = "BASICRATEOFINVOICETAX")
    var basicRateOfInvoiceTax: String?
)
