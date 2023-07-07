package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

/**
 * Created by paripranu on 06/13/18.
 */
@XmlAccessorType(XmlAccessType.NONE)
class InventoryList {
    @XmlElement(name = "STOCKITEMNAME")
    var stockItemName = ""

    @XmlElement(name = "ISDEEMEDPOSITIVE")
    var isDeemedPositive = ""

    @XmlElement(name = "ISLASTDEEMEDPOSITIVE")
    var isLastDeemedPositive = ""

    @XmlElement(name = "RATE")
    var rate = ""

    @XmlElement(name = "AMOUNT")
    var amount = ""

    @XmlElement(name = "ACTUALQTY")
    var actualQty = ""

    @XmlElement(name = "BILLEDQTY")
    var billedQty = ""

    @XmlElement(name = "GODOWNNAME")
    var godownName: String? = null

    @XmlElement(name = "DISOCUNT")
    var discount = 0.0

    @XmlElement(name = "ACCOUNTINGALLOCATIONS.LIST")
    var accountingAllocationList: List<AccountingAllocation>? = null

    @XmlElement(name = "BATCHALLOCATIONS.LIST")
    var batchAllocationsList: List<BatchAllocation>? = null

    companion object {
        const val MAIN_LOCATION = "Main Location"
        const val DAMAGE_LOCATION = "Damage Location"
        const val PRIMARY_BATCH = "Primary Batch"
    }
}
