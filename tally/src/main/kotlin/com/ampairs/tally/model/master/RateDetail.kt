package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
class RateDetail {
    @XmlElement(name = "GSTRATEDUTYHEAD")
    var gstRateDutyHead: String? = null

    @XmlElement(name = "GSTRATEVALUATIONTYPE")
    var gstRateValuationType: String? = null

    @XmlElement(name = "GSTRATE")
    var gstRate: String? = null
}
