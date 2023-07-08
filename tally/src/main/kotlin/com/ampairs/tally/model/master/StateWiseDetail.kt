package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class StateWiseDetail(
    @field:XmlElement(name = "STATENAME")
    var stateName: String? = null,

    @field:XmlElement(name = "RATEDETAILS.LIST", type = RateDetail::class)
    var rateDetailsList: List<RateDetail>? = null,

    @field:XmlElement(name = "GSTSLABRATES.LIST")
    var gstsLabrates: String? = null,
)
