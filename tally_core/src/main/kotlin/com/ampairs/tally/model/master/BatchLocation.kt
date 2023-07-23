package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class BatchLocation(
    @field:XmlElement(name = "GODOWNNAME")
    var godownName: String? = null,

    @field:XmlElement(name = "BATCHNAME")
    var batchName: String? = null,

    @field:XmlElement(name = "OPENINGBALANCE")
    var openingBalance: String? = null,

    @field:XmlElement(name = "OPENINGVALUE")
    var openingValue: String? = null,

    @field:XmlElement(name = "OPENINGRATE")
    var openingRate: String? = null,
)
