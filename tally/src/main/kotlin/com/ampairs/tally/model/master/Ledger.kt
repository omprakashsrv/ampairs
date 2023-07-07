package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
class Ledger {
    @XmlAttribute(name = "NAME")
    var name = ""

    @XmlAttribute(name = "RESERVEDNAME")
    var reservedName = ""

    @XmlElement(name = "NAME.LIST", type = Name::class)
    var nameList: List<Name>? = null

    @XmlElement(name = "ADDRESS.LIST", type = Address::class)
    var addressList: List<Address>? = null

    @XmlElement(name = "ADDITIONALNAME")
    var additionalName: String? = null

    @XmlElement(name = "CURRENCYNAME")
    var currencyName: String? = null

    @XmlElement(name = "STATENAME")
    var stateName: String? = null

    @XmlElement(name = "BANKACCHOLDERNAME")
    var bankAccHolderName: String? = null

    @XmlElement(name = "LEDSTATENAME")
    var ledStateName: String? = null

    @XmlElement(name = "PARTYGSTIN")
    var partyGstin: String? = null

    @XmlElement(name = "GUID")
    var guid: String? = null

    @XmlElement(name = "PINCODE")
    var pinCode: String? = null

    @XmlElement(name = "COUNTRYNAME")
    var countryName: String? = null

    @XmlElement(name = "GSTREGISTRATIONTYPE")
    var gstRegistrationType: String? = null

    @XmlElement(name = "IFSCODE")
    var ifsCode: String? = null

    @XmlElement(name = "BANKDETAILS")
    var bankDetails: String? = null

    @XmlElement(name = "BANKBRANCHNAME")
    var bankBranchName: String? = null

    @XmlElement(name = "COUNTRYOFRESIDENCE")
    var countryOfResidence: String? = null

    @XmlElement(name = "INCOMETAXNUMBER")
    var incomeTaxNumber: String? = null

    @XmlElement(name = "PANAPPLICABLEFROM")
    var panApplicableFrom: String? = null

    @XmlElement(name = "NAMEONPAN")
    var nameOnPAN: String? = null

    @XmlElement(name = "SALESTAXNUMBER")
    var salesTaxNumber: String? = null

    @XmlElement(name = "VATTINNUMBER")
    var vatTinNumber: String? = null

    @XmlElement(name = "PARENT")
    var parent: String? = null

    @XmlElement(name = "GSTDUTYHEAD")
    var gstDutyHead: String? = null

    @XmlElement(name = "RATEOFTAXCALCULATION")
    var rateOfTaxCalculation: String? = null

    @XmlElement(name = "SERVICECATEGORY")
    var serviceCategory: String? = null

    @XmlElement(name = "TAXTYPE")
    var taxType: String? = null

    @XmlElement(name = "GSTAPPLICABLE")
    var gstApplicable: String? = null

    @XmlElement(name = "GSTTYPEOFSUPPLY")
    var gstTypeofSupply: String? = null

    @XmlElement(name = "TRADERLEDNATUREOFPURCHASE")
    var traderLedNatureOfPurchase: String? = null

    @XmlElement(name = "TDSDEDUCTEETYPE")
    var tdsDeducteeType: String? = null

    @XmlElement(name = "TDSRATENAME")
    var tdsRateName: String? = null

    @XmlElement(name = "LEDGERFBTCATEGORY")
    var ledgerFBTCategory: String? = null

    @XmlElement(name = "ISBILLWISEON")
    var isBillWiseOn: String? = null

    @XmlElement(name = "ISCOSTCENTRESON")
    var isCostCentresOn: String? = null

    @XmlElement(name = "ISINTERESTON")
    var isInterestOn: String? = null

    @XmlElement(name = "ALLOWINMOBILE")
    var allowInMobile: String? = null

    @XmlElement(name = "ISCONDENSED")
    var isCondensed: String? = null

    @XmlElement(name = "AFFECTSSTOCK")
    var affectsStock: String? = null

    @XmlElement(name = "FORPAYROLL")
    var forPayRoll: String? = null

    @XmlElement(name = "INTERESTONBILLWISE")
    var interestOnBillWise: String? = null

    @XmlElement(name = "OVERRIDEINTEREST")
    var overrideInterest: String? = null

    @XmlElement(name = "OVERRIDEADVINTEREST")
    var overrideAdvInterest: String? = null

    @XmlElement(name = "USEFORVAT")
    var useForVat: String? = null

    @XmlElement(name = "ISTCSAPPLICABLE")
    var isTCSApplicable: String? = null

    @XmlElement(name = "TCSAPPLICABLE")
    var tcsApplicable: String? = null

    @XmlElement(name = "ISTDSAPPLICABLE")
    var isTDSApplicable: String? = null

    @XmlElement(name = "ISFBTAPPLICABLE")
    var isFBTApplicable: String? = null

    @XmlElement(name = "ISGSTAPPLICABLE")
    var isGSTApplicable: String? = null

    @XmlElement(name = "SHOWINPAYSLIP")
    var showInPayslip: String? = null

    @XmlElement(name = "USEFORGRATUITY")
    var useForGratuity: String? = null

    @XmlElement(name = "FORSERVICETAX")
    var forServiceTax: String? = null

    @XmlElement(name = "ISINPUTCREDIT")
    var isInputCredit: String? = null

    @XmlElement(name = "ISEXEMPTED")
    var isExempted: String? = null

    @XmlElement(name = "TDSDEDUCTEEISSPECIALRATE")
    var tdsDeducteeIsSpecialRate: String? = null

    @XmlElement(name = "AUDITED")
    var audited: String? = null

    @XmlElement(name = "ASORIGINAL")
    var asOriginal: String? = null

    @XmlElement(name = "SORTPOSITION")
    var sortPosition: String? = null

    @XmlElement(name = "TCSCATEGORYDETAILS.LIST")
    private var tcsCategoryDetailList: MutableList<TCSCategoryDetail>? = null
}
