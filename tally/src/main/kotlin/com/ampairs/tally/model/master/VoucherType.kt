package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
class VoucherType {
    @XmlAttribute(name = "NAME")
    var name = ""

    @XmlAttribute(name = "RESERVEDNAME")
    var reservedName = ""

    @XmlElement(name = "NAME.LIST", type = Name::class)
    var nameList: List<Name>? = null

    @XmlElement(name = "ADDITIONALNAME")
    var additionalName: String? = null

    @XmlElement(name = "PARENT")
    var parent: String? = null

    @XmlElement(name = "NUMBERINGMETHOD")
    var numberingMethod: String? = null

    @XmlElement(name = "ISDEEMEDPOSITIVE")
    var isDeemedPositive: String? = null

    @XmlElement(name = "AFFECTSTOCK")
    var affectStock: String? = null

    @XmlElement(name = "PREVENTDUPLICATES")
    var preventDuplicates: String? = null

    @XmlElement(name = "PREFILLZERO")
    var prefillZero: String? = null

    @XmlElement(name = "PRINTAFTERSAVE")
    var printAfterSave: String? = null

    @XmlElement(name = "FORMALRECEIPT")
    var formalReceipt: String? = null

    @XmlElement(name = "ISOPTIONAL")
    var isOptional: String? = null

    @XmlElement(name = "ASMFGJRNL")
    var asmfgjrnl: String? = null

    @XmlElement(name = "EFFECTIVEDATE")
    var effectiveDate: String? = null

    @XmlElement(name = "COMMONNARRATION")
    var commonNarration: String? = null

    @XmlElement(name = "MULTINARRATION")
    var multiNarration: String? = null

    @XmlElement(name = "ISTAXINVOICE")
    var isTaxInvoice: String? = null

    @XmlElement(name = "USEFORPOSINVOICE")
    var useForPosInvoice: String? = null

    @XmlElement(name = "USEFOREXCISETRADINGINVOICE")
    var useForExciseTradingInvoice: String? = null

    @XmlElement(name = "SORTPOSITION")
    var sortPosition: String? = null

    @XmlElement(name = "BEGINNINGNUMBER")
    var beginningNumber: String? = null
}
