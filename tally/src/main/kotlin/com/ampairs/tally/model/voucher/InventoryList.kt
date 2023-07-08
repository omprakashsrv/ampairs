package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement


@XmlAccessorType(XmlAccessType.FIELD)
data class InventoryList(
    @field:XmlElement(name = "STOCKITEMNAME")
    var stockItemName: String? = null,

    @field:XmlElement(name = "ISDEEMEDPOSITIVE")
    var isDeemedPositive: String? = null,

    @field:XmlElement(name = "ISLASTDEEMEDPOSITIVE")
    var isLastDeemedPositive: String? = null,

    @field:XmlElement(name = "RATE")
    var rate: String? = null,

    @field:XmlElement(name = "AMOUNT")
    var amount: String? = null,

    @field:XmlElement(name = "ACTUALQTY")
    var actualQty: String? = null,

    @field:XmlElement(name = "BILLEDQTY")
    var billedQty: String? = null,

    @field:XmlElement(name = "GODOWNNAME")
    var godownName: String? = null,

    @field:XmlElement(name = "DISOCUNT")
    var discount: Double,

    @field:XmlElement(name = "ACCOUNTINGALLOCATIONS.LIST")
    var accountingAllocationList: List<AccountingAllocation>? = null,

    @field:XmlElement(name = "BATCHALLOCATIONS.LIST")
    var batchAllocationsList: List<BatchAllocation>? = null,

    )
