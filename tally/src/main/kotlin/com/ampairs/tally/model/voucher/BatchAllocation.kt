package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = ["godownName", "batchName", "destinationGodownName", "amount", "actualQty", "billedQty"])
class BatchAllocation {
    @XmlElement(name = "GODOWNNAME")
    var godownName = "Main Location"

    @XmlElement(name = "BATCHNAME")
    var batchName = "Primary Batch"

    @XmlElement(name = "DESTINATIONGODOWNNAME")
    var destinationGodownName = "Main Location"

    @XmlElement(name = "AMOUNT")
    var amount: String? = null

    @XmlElement(name = "ACTUALQTY")
    var actualQty: String? = null

    @XmlElement(name = "BILLEDQTY")
    var billedQty: String? = null
}
