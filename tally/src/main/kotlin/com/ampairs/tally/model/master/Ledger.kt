package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class Ledger(
    @XmlAttribute(name = "NAME")
    var name: String? = null,

    @XmlAttribute(name = "RESERVEDNAME")
    var reservedName: String? = null,

    @field:XmlElement(name = "NAME.LIST", type = Name::class)
    var nameList: List<Name>? = null,

    @field:XmlElement(name = "ADDRESS.LIST", type = Address::class)
    var addressList: List<Address>? = null,

    @field:XmlElement(name = "ADDITIONALNAME")
    var additionalName: String? = null,

    @field:XmlElement(name = "CURRENCYNAME")
    var currencyName: String? = null,

    @field:XmlElement(name = "STATENAME")
    var stateName: String? = null,

    @field:XmlElement(name = "BANKACCHOLDERNAME")
    var bankAccHolderName: String? = null,

    @field:XmlElement(name = "LEDSTATENAME")
    var ledStateName: String? = null,

    @field:XmlElement(name = "PARTYGSTIN")
    var partyGstin: String? = null,

    @field:XmlElement(name = "GUID")
    var guid: String? = null,

    @field:XmlElement(name = "PINCODE")
    var pinCode: String? = null,

    @field:XmlElement(name = "COUNTRYNAME")
    var countryName: String? = null,

    @field:XmlElement(name = "GSTREGISTRATIONTYPE")
    var gstRegistrationType: String? = null,

    @field:XmlElement(name = "IFSCODE")
    var ifsCode: String? = null,

    @field:XmlElement(name = "BANKDETAILS")
    var bankDetails: String? = null,

    @field:XmlElement(name = "BANKBRANCHNAME")
    var bankBranchName: String? = null,

    @field:XmlElement(name = "COUNTRYOFRESIDENCE")
    var countryOfResidence: String? = null,

    @field:XmlElement(name = "INCOMETAXNUMBER")
    var incomeTaxNumber: String? = null,

    @field:XmlElement(name = "PANAPPLICABLEFROM")
    var panApplicableFrom: String? = null,

    @field:XmlElement(name = "NAMEONPAN")
    var nameOnPAN: String? = null,

    @field:XmlElement(name = "SALESTAXNUMBER")
    var salesTaxNumber: String? = null,

    @field:XmlElement(name = "VATTINNUMBER")
    var vatTinNumber: String? = null,

    @field:XmlElement(name = "PARENT")
    var parent: String? = null,

    @field:XmlElement(name = "GSTDUTYHEAD")
    var gstDutyHead: String? = null,

    @field:XmlElement(name = "RATEOFTAXCALCULATION")
    var rateOfTaxCalculation: String? = null,

    @field:XmlElement(name = "SERVICECATEGORY")
    var serviceCategory: String? = null,

    @field:XmlElement(name = "TAXTYPE")
    var taxType: String? = null,

    @field:XmlElement(name = "GSTAPPLICABLE")
    var gstApplicable: String? = null,

    @field:XmlElement(name = "GSTTYPEOFSUPPLY")
    var gstTypeofSupply: String? = null,

    @field:XmlElement(name = "TRADERLEDNATUREOFPURCHASE")
    var traderLedNatureOfPurchase: String? = null,

    @field:XmlElement(name = "TDSDEDUCTEETYPE")
    var tdsDeducteeType: String? = null,

    @field:XmlElement(name = "TDSRATENAME")
    var tdsRateName: String? = null,

    @field:XmlElement(name = "LEDGERFBTCATEGORY")
    var ledgerFBTCategory: String? = null,

    @field:XmlElement(name = "ISBILLWISEON")
    var isBillWiseOn: String? = null,

    @field:XmlElement(name = "ISCOSTCENTRESON")
    var isCostCentresOn: String? = null,

    @field:XmlElement(name = "ISINTERESTON")
    var isInterestOn: String? = null,

    @field:XmlElement(name = "ALLOWINMOBILE")
    var allowInMobile: String? = null,

    @field:XmlElement(name = "ISCONDENSED")
    var isCondensed: String? = null,

    @field:XmlElement(name = "AFFECTSSTOCK")
    var affectsStock: String? = null,

    @field:XmlElement(name = "FORPAYROLL")
    var forPayRoll: String? = null,

    @field:XmlElement(name = "INTERESTONBILLWISE")
    var interestOnBillWise: String? = null,

    @field:XmlElement(name = "OVERRIDEINTEREST")
    var overrideInterest: String? = null,

    @field:XmlElement(name = "OVERRIDEADVINTEREST")
    var overrideAdvInterest: String? = null,

    @field:XmlElement(name = "USEFORVAT")
    var useForVat: String? = null,

    @field:XmlElement(name = "ISTCSAPPLICABLE")
    var isTCSApplicable: String? = null,

    @field:XmlElement(name = "TCSAPPLICABLE")
    var tcsApplicable: String? = null,

    @field:XmlElement(name = "ISTDSAPPLICABLE")
    var isTDSApplicable: String? = null,

    @field:XmlElement(name = "ISFBTAPPLICABLE")
    var isFBTApplicable: String? = null,

    @field:XmlElement(name = "ISGSTAPPLICABLE")
    var isGSTApplicable: String? = null,

    @field:XmlElement(name = "SHOWINPAYSLIP")
    var showInPayslip: String? = null,

    @field:XmlElement(name = "USEFORGRATUITY")
    var useForGratuity: String? = null,

    @field:XmlElement(name = "FORSERVICETAX")
    var forServiceTax: String? = null,

    @field:XmlElement(name = "ISINPUTCREDIT")
    var isInputCredit: String? = null,

    @field:XmlElement(name = "ISEXEMPTED")
    var isExempted: String? = null,

    @field:XmlElement(name = "TDSDEDUCTEEISSPECIALRATE")
    var tdsDeducteeIsSpecialRate: String? = null,

    @field:XmlElement(name = "AUDITED")
    var audited: String? = null,

    @field:XmlElement(name = "ASORIGINAL")
    var asOriginal: String? = null,

    @field:XmlElement(name = "SORTPOSITION")
    var sortPosition: String? = null,

    @field:XmlElement(name = "LEDGERMOBILE")
    var ledgerMobile: String? = null,

    @field:XmlElement(name = "LEDGERPHONE")
    var ledgerPhone: String? = null,

    @field:XmlElement(name = "TCSCATEGORYDETAILS.LIST")
    private var tcsCategoryDetailList: MutableList<TCSCategoryDetail>? = null,
)
