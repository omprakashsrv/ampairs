package com.ampairs.tally.model.voucher

import com.ampairs.tally.model.master.RateDetail
import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
class LedgerEntrie(
    @field:XmlElement(name = "LEDGERNAME")
    var ledgerName: String? = null,

    @field:XmlElement(name = "ISDEEMEDPOSITIVE")
    private var isDeemedPositive: String? = null,

    @field:XmlElement(name = "ISPARTYLEDGER")
    var isPartyLedger: String? = null,

    @field:XmlElement(name = "ISLASTDEEMEDPOSITIVE")
    private var isLastDeemedPositive: String? = null,

    @field:XmlElement(name = "AMOUNT")
    var amount: String? = null,

    @field:XmlElement(name = "GSTOVRDNNATURE")
    var gstOvrdnNature: String? = null,

    @field:XmlElement(name = "BANKALLOCATIONS.LIST", type = BankAllocation::class)
    var bankAllocationList: List<BankAllocation>? = null,

    @field:XmlElement(name = "BILLALLOCATIONS.LIST", type = BillAllocation::class)
    var billAllocation: BillAllocation? = null,

    @field:XmlElement(name = "TAXOBJECTALLOCATIONS.LIST", type = TaxAllocation::class)
    var taxAllocation: TaxAllocation? = null,

    @field:XmlElement(name = "BASICRATEOFINVOICETAX.LIST", type = BasicRateOfInvoiceTax::class)
    var basicRateOfInvoiceTax: BasicRateOfInvoiceTax? = null,

    @field:XmlElement(name = "INVENTORYALLOCATIONS.LIST")
    var inventoryAllocations: List<InventoryAllocation>? = null,

    @field:XmlElement(name = "RATEDETAILS.LIST", type = RateDetail::class)
    var rateDetailsList: List<RateDetail>? = null,

    )