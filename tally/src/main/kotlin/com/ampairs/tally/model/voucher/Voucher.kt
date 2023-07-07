package com.ampairs.tally.model.voucher

import com.ampairs.tally.model.master.*
import jakarta.xml.bind.annotation.*

@XmlAccessorType(XmlAccessType.NONE)
class Voucher {
    @XmlAttribute(name = "REMOTEID")
    var remoteId: String? = null

    @XmlAttribute(name = "VCHTYPE")
    var vchType = VoucherName.Sales

    @XmlAttribute(name = "ACTION")
    var action = VoucherAction.Alter

    @XmlElement(name = "DATE")
    var date: String? = null

    @XmlElement(name = "REFERENCEDATE")
    var referenceDate: String? = null

    @XmlElement(name = "INVDELIVERYDATE")
    var invoiceDeliveryDate: String? = null

    @XmlElement(name = "GUID")
    var guid: String? = null

    @XmlElement(name = "STATENAME")
    var stateName: String? = null

    @XmlElement(name = "COUNTRYOFRESIDENCE")
    var countryOfResidence = "INDIA"

    @XmlElement(name = "PARTYGSTIN")
    var partyGstin: String? = null

    @XmlElement(name = "PARTYNAME")
    var partyName: String? = null

    @XmlElement(name = "VOUCHERTYPENAME")
    var voucherTypeName: String? = null

    @XmlElement(name = "REFERENCE")
    var referenceNumber: String? = null

    @XmlElement(name = "VOUCHERNUMBER")
    var voucherNumber: String? = null

    @XmlElement(name = "PARTYLEDGERNAME")
    var partyLedgerName: String? = null

    @XmlElement(name = "BASICBASEPARTYNAME")
    var basicBasePartyName: String? = null

    @XmlElement(name = "PLACEOFSUPPLY")
    var placeOfSupply: String? = null

    @XmlElement(name = "CONSIGNEEGSTIN")
    var consigneeGstin = ""

    @XmlElement(name = "BASICBUYERNAME")
    var basicBuyerName: String? = null

    @XmlElement(name = "BASICDATETIMEOFINVOICE")
    var basicDateTimeOfInvoice: String? = null

    @XmlElement(name = "BASICDATETIMEOFREMOVAL")
    var basicDateTimeOfRemoval: String? = null

    @XmlElement(name = "CONSIGNEEPINNUMBER")
    var consigneePinNumber: String? = null

    @XmlElement(name = "CONSIGNEESTATENAME")
    var consigneeStateName: String? = null

    @XmlElement(name = "EFFECTIVEDATE")
    var effectiveDate: String? = null

    @XmlElement(name = "ISINVOICE")
    var isInvoice = "Yes"

    @XmlElement(name = "LEDGERENTRIES.LIST", type = LedgerEntrie::class)
    var ledgerEntrieList: List<LedgerEntrie>? = null

    @XmlElement(name = "ALLLEDGERENTRIES.LIST", type = LedgerEntrie::class)
    var allLedgerEntriesList: List<LedgerEntrie>? = null

    @XmlElementWrapper(name = "ALLINVENTORYENTRIES.LIST")
    @XmlElement(name = "ALLINVENTORYENTRIES.LIST")
    var inventoryList: List<InventoryList>? = null

    @XmlElementWrapper(name = "INVENTORYALLOCATIONS.LIST")
    @XmlElement(name = "INVENTORYALLOCATIONS.LIST")
    var inventoryAllocations: List<InventoryAllocation>? = null

    @XmlElement(name = "ADDRESS.LIST", type = Address::class)
    var addressList: List<Address>? = null

    @XmlElement(name = "BASICBUYERADDRESS.LIST", type = BasicBuyerAddress::class)
    var basicBuyerAddressList: List<BasicBuyerAddress>? = null

    @XmlElement(name = "NARRATION")
    var narration: String? = null

    @XmlElement(name = "GSTNATUREOFRETURN")
    var natureOfReturn: String? = null
}
