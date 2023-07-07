package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
data class BatchLocation(
    @XmlElement(name = "GODOWNNAME")
    var godownName: String?,

    @XmlElement(name = "BATCHNAME")
    var batchName: String?,

    @XmlElement(name = "OPENINGBALANCE")
    var openingBalance: String?,

    @XmlElement(name = "OPENINGVALUE")
    var openingValue: String?,

    @XmlElement(name = "OPENINGRATE")
    var openingRate: String?,
)
