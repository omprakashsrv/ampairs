package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class RateDetail(
    @field:XmlElement(name = "GSTRATEDUTYHEAD")
    var gstRateDutyHead: String? = null,

    @field:XmlElement(name = "GSTRATEVALUATIONTYPE")
    var gstRateValuationType: String? = null,

    @field:XmlElement(name = "GSTRATE")
    var gstRate: String? = null,
)
