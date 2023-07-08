package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class VoucherType(
    @XmlAttribute(name = "NAME")
    var name: String? = null,

    @XmlAttribute(name = "RESERVEDNAME")
    var reservedName: String? = null,

    @field:XmlElement(name = "NAME.LIST", type = Name::class)
    var nameList: List<Name>? = null,

    @field:XmlElement(name = "ADDITIONALNAME")
    var additionalName: String? = null,

    @field:XmlElement(name = "PARENT")
    var parent: String? = null,

    @field:XmlElement(name = "NUMBERINGMETHOD")
    var numberingMethod: String? = null,

    @field:XmlElement(name = "ISDEEMEDPOSITIVE")
    var isDeemedPositive: String? = null,

    @field:XmlElement(name = "AFFECTSTOCK")
    var affectStock: String? = null,

    @field:XmlElement(name = "PREVENTDUPLICATES")
    var preventDuplicates: String? = null,

    @field:XmlElement(name = "PREFILLZERO")
    var prefillZero: String? = null,

    @field:XmlElement(name = "PRINTAFTERSAVE")
    var printAfterSave: String? = null,

    @field:XmlElement(name = "FORMALRECEIPT")
    var formalReceipt: String? = null,

    @field:XmlElement(name = "ISOPTIONAL")
    var isOptional: String? = null,

    @field:XmlElement(name = "ASMFGJRNL")
    var asmfgjrnl: String? = null,

    @field:XmlElement(name = "EFFECTIVEDATE")
    var effectiveDate: String? = null,

    @field:XmlElement(name = "COMMONNARRATION")
    var commonNarration: String? = null,

    @field:XmlElement(name = "MULTINARRATION")
    var multiNarration: String? = null,

    @field:XmlElement(name = "ISTAXINVOICE")
    var isTaxInvoice: String? = null,

    @field:XmlElement(name = "USEFORPOSINVOICE")
    var useForPosInvoice: String? = null,

    @field:XmlElement(name = "USEFOREXCISETRADINGINVOICE")
    var useForExciseTradingInvoice: String? = null,

    @field:XmlElement(name = "SORTPOSITION")
    var sortPosition: String? = null,

    @field:XmlElement(name = "BEGINNINGNUMBER")
    var beginningNumber: String? = null,
)
