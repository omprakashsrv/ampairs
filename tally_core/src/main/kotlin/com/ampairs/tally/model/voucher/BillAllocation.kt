package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = ["name", "billType", "billCreditPeriod", "tdsDeducteeIsSpecialRate", "amount"])
data class BillAllocation(
    @field:XmlElement(name = "NAME")
    var name: String? = null,

    @field:XmlElement(name = "BILLTYPE")
    var billType: String? = null,

    @field:XmlElement(name = "BILLCREDITPERIOD")
    var billCreditPeriod: String? = null,

    @field:XmlElement(name = "TDSDEDUCTEEISSPECIALRATE")
    var tdsDeducteeIsSpecialRate: String? = null,

    @field:XmlElement(name = "AMOUNT")
    var amount: String? = null,
)
