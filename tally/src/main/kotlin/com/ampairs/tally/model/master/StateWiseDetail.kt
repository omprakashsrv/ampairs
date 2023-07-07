package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
class StateWiseDetail {
    @XmlElement(name = "STATENAME")
    var stateName = "Any"

    @XmlElement(name = "RATEDETAILS.LIST", type = RateDetail::class)
    var rateDetailsList: List<RateDetail>? = null

    @XmlElement(name = "GSTSLABRATES.LIST")
    var gstsLabrates: String? = null
}
