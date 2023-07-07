package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
data class CategoryDetail(
    @XmlElement(name = "APPLICABLEFROM")
    var applicableFrom: String?,

    @XmlElement(name = "ISZERORATED")
    var isZeroRated: String?,

    @XmlElement(name = "ISNORMALRATEAPPLICABLE")
    var isNormalRateApplicable: String?,

    @XmlElement(name = "RATEOFTAX")
    var rateOfTax: String?,

    @XmlElement(name = "OTHERRATE")
    var otherRate: String?,

    @XmlElement(name = "NOPANRATEOFTAX")
    var noPANRateOfTax: String?,

    @XmlElement(name = "NOPANOTHERRATE")
    var noPANOtherRate: String?,
)
