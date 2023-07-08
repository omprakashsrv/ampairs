package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = ["isDeemedPositive", "isLastDeemedPositive", "stockItemName", "amount", "actualQty", "billedQty", "rate", "batchAllocations", "discount"])
class InventoryAllocation(
    @field:XmlElement(name = "ISDEEMEDPOSITIVE")
    var isDeemedPositive: String? = null,

    @field:XmlElement(name = "ISLASTDEEMEDPOSITIVE")
    var isLastDeemedPositive: String? = null,

    @field:XmlElement(name = "STOCKITEMNAME")
    var stockItemName: String? = null,

    @field:XmlElement(name = "AMOUNT")
    var amount: String? = null,

    @field:XmlElement(name = "ACTUALQTY")
    var actualQty: String? = null,

    @field:XmlElement(name = "BILLEDQTY")
    var billedQty: String? = null,

    @field:XmlElement(name = "RATE")
    var rate: String? = null,

    @field:XmlElement(name = "DISCOUNT")
    var discount: String? = null,

    @field:XmlElement(name = "BATCHALLOCATIONS.LIST", type = BatchAllocation::class)
    var batchAllocations: List<BatchAllocation>,
)
