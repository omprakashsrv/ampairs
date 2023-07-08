package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = ["godownName", "batchName", "destinationGodownName", "amount", "actualQty", "billedQty"])
data class BatchAllocation(
    @field:XmlElement(name = "GODOWNNAME")
    var godownName: String? = null,

    @field:XmlElement(name = "BATCHNAME")
    var batchName: String? = null,

    @field:XmlElement(name = "DESTINATIONGODOWNNAME")
    var destinationGodownName: String? = null,

    @field:XmlElement(name = "AMOUNT")
    var amount: String? = null,

    @field:XmlElement(name = "ACTUALQTY")
    var actualQty: String? = null,

    @field:XmlElement(name = "BILLEDQTY")
    var billedQty: String? = null,
)
