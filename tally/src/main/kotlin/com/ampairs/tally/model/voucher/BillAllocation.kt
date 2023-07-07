package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = ["name", "billType", "billCreditPeriod", "tdsDeducteeIsSpecialRate", "amount"])
class BillAllocation {
    @XmlElement(name = "NAME")
    var name: String? = null

    @XmlElement(name = "BILLTYPE")
    var billType: String? = null

    @XmlElement(name = "BILLCREDITPERIOD")
    var billCreditPeriod: String? = null

    @XmlElement(name = "TDSDEDUCTEEISSPECIALRATE")
    var tdsDeducteeIsSpecialRate: String? = null

    @XmlElement(name = "AMOUNT")
    var amount: String? = null
}
