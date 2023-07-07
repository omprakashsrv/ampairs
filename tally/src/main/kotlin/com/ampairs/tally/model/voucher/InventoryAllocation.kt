package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = ["isDeemedPositive", "isLastDeemedPositive", "stockItemName", "amount", "actualQty", "billedQty", "rate", "batchAllocations", "discount"])
class InventoryAllocation {
    @XmlElement(name = "ISDEEMEDPOSITIVE")
    var isDeemedPositive: String? = null
        set(isDeemedPositive) {
            field = isDeemedPositive
            isLastDeemedPositive = isDeemedPositive
        }

    @XmlElement(name = "ISLASTDEEMEDPOSITIVE")
    var isLastDeemedPositive: String? = null

    @XmlElement(name = "STOCKITEMNAME")
    var stockItemName: String? = null

    @XmlElement(name = "AMOUNT")
    var amount: String? = null

    @XmlElement(name = "ACTUALQTY")
    var actualQty: String? = null

    @XmlElement(name = "BILLEDQTY")
    var billedQty: String? = null

    @XmlElement(name = "RATE")
    var rate: String? = null

    @XmlElement(name = "DISCOUNT")
    var discount: String? = null

    @XmlElement(name = "BATCHALLOCATIONS.LIST", type = BatchAllocation::class)
    var batchAllocations: List<BatchAllocation> = ArrayList()
}
