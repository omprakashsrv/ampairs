package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class CategoryDetail(
    @field:XmlElement(name = "APPLICABLEFROM")
    var applicableFrom: String? = null,

    @field:XmlElement(name = "ISZERORATED")
    var isZeroRated: String? = null,

    @field:XmlElement(name = "ISNORMALRATEAPPLICABLE")
    var isNormalRateApplicable: String? = null,

    @field:XmlElement(name = "RATEOFTAX")
    var rateOfTax: String? = null,

    @field:XmlElement(name = "OTHERRATE")
    var otherRate: String? = null,

    @field:XmlElement(name = "NOPANRATEOFTAX")
    var noPANRateOfTax: String? = null,

    @field:XmlElement(name = "NOPANOTHERRATE")
    var noPANOtherRate: String? = null,
)
