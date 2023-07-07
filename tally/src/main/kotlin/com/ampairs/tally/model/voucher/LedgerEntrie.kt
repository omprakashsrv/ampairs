package com.ampairs.tally.model.voucher

import com.ampairs.tally.model.master.RateDetail
import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
class LedgerEntrie {
    @XmlElement(name = "LEDGERNAME")
    var ledgerName: String? = null

    @XmlElement(name = "ISDEEMEDPOSITIVE")
    private var isDeemedPositive: String? = null

    @XmlElement(name = "ISPARTYLEDGER")
    var isPartyLedger: String? = null

    @XmlElement(name = "ISLASTDEEMEDPOSITIVE")
    private var isLastDeemedPositive: String? = null

    @XmlElement(name = "AMOUNT")
    var amount: String? = null

    @XmlElement(name = "GSTOVRDNNATURE")
    var gstOvrdnNature: String? = null

    @XmlElement(name = "BANKALLOCATIONS.LIST", type = BankAllocation::class)
    var bankAllocationList: List<BankAllocation>? = null

    @XmlElement(name = "BILLALLOCATIONS.LIST", type = BillAllocation::class)
    var billAllocation: BillAllocation? = null

    @XmlElement(name = "TAXOBJECTALLOCATIONS.LIST", type = TaxAllocation::class)
    var taxAllocation: TaxAllocation? = null

    @XmlElement(name = "BASICRATEOFINVOICETAX.LIST", type = BasicRateOfInvoiceTax::class)
    var basicRateOfInvoiceTax: BasicRateOfInvoiceTax? = null

    @XmlElement(name = "INVENTORYALLOCATIONS.LIST")
    var inventoryAllocations: List<InventoryAllocation>? = null

    @XmlElement(name = "RATEDETAILS.LIST", type = RateDetail::class)
    var rateDetailsList: List<RateDetail>? = null
    fun getIsDeemedPositive(): String? {
        return isDeemedPositive
    }

    fun setIsDeemedPositive(isDeemedPositive: String?) {
        this.isDeemedPositive = isDeemedPositive
        isLastDeemedPositive = isDeemedPositive
    }

    fun getIsLastDeemedPositive(): String? {
        return isLastDeemedPositive
    }

    fun setIsLastDeemedPositive(isLastDeemedPositive: String?) {
        isDeemedPositive = isLastDeemedPositive
        this.isLastDeemedPositive = isLastDeemedPositive
    }
}