package com.ampairs.tally.model.voucher

import com.ampairs.tally.model.master.*
import jakarta.xml.bind.annotation.*

@XmlAccessorType(XmlAccessType.FIELD)
data class Voucher(
    @XmlAttribute(name = "REMOTEID")
    var remoteId: String? = null,

    @XmlAttribute(name = "VCHTYPE")
    var vchType: VoucherName,

    @XmlAttribute(name = "ACTION")
    var action: VoucherAction,

    @field:XmlElement(name = "DATE")
    var date: String? = null,

    @field:XmlElement(name = "REFERENCEDATE")
    var referenceDate: String? = null,

    @field:XmlElement(name = "INVDELIVERYDATE")
    var invoiceDeliveryDate: String? = null,

    @field:XmlElement(name = "GUID")
    var guid: String? = null,

    @field:XmlElement(name = "STATENAME")
    var stateName: String? = null,

    @field:XmlElement(name = "COUNTRYOFRESIDENCE")
    var countryOfResidence: String? = null,

    @field:XmlElement(name = "PARTYGSTIN")
    var partyGstin: String? = null,

    @field:XmlElement(name = "PARTYNAME")
    var partyName: String? = null,

    @field:XmlElement(name = "VOUCHERTYPENAME")
    var voucherTypeName: String? = null,

    @field:XmlElement(name = "REFERENCE")
    var referenceNumber: String? = null,

    @field:XmlElement(name = "VOUCHERNUMBER")
    var voucherNumber: String? = null,

    @field:XmlElement(name = "PARTYLEDGERNAME")
    var partyLedgerName: String? = null,

    @field:XmlElement(name = "BASICBASEPARTYNAME")
    var basicBasePartyName: String? = null,

    @field:XmlElement(name = "PLACEOFSUPPLY")
    var placeOfSupply: String? = null,

    @field:XmlElement(name = "CONSIGNEEGSTIN")
    var consigneeGstin: String? = null,

    @field:XmlElement(name = "BASICBUYERNAME")
    var basicBuyerName: String? = null,

    @field:XmlElement(name = "BASICDATETIMEOFINVOICE")
    var basicDateTimeOfInvoice: String? = null,

    @field:XmlElement(name = "BASICDATETIMEOFREMOVAL")
    var basicDateTimeOfRemoval: String? = null,

    @field:XmlElement(name = "CONSIGNEEPINNUMBER")
    var consigneePinNumber: String? = null,

    @field:XmlElement(name = "CONSIGNEESTATENAME")
    var consigneeStateName: String? = null,

    @field:XmlElement(name = "EFFECTIVEDATE")
    var effectiveDate: String? = null,

    @field:XmlElement(name = "ISINVOICE")
    var isInvoice: String? = null,

    @field:XmlElement(name = "LEDGERENTRIES.LIST", type = LedgerEntrie::class)
    var ledgerEntrieList: List<LedgerEntrie>? = null,

    @field:XmlElement(name = "ALLLEDGERENTRIES.LIST", type = LedgerEntrie::class)
    var allLedgerEntriesList: List<LedgerEntrie>? = null,

    @field:XmlElementWrapper(name = "ALLINVENTORYENTRIES.LIST")
    @field:XmlElement(name = "ALLINVENTORYENTRIES.LIST")
    var inventoryList: List<InventoryList>? = null,

    @field:XmlElementWrapper(name = "INVENTORYALLOCATIONS.LIST")
    @field:XmlElement(name = "INVENTORYALLOCATIONS.LIST")
    var inventoryAllocations: List<InventoryAllocation>? = null,

    @field:XmlElement(name = "ADDRESS.LIST", type = Address::class)
    var addressList: List<Address>? = null,

    @field:XmlElement(name = "BASICBUYERADDRESS.LIST", type = BasicBuyerAddress::class)
    var basicBuyerAddressList: List<BasicBuyerAddress>? = null,

    @field:XmlElement(name = "NARRATION")
    var narration: String? = null,

    @field:XmlElement(name = "GSTNATUREOFRETURN")
    var natureOfReturn: String? = null,
)
