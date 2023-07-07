package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
class StandardCost {
    @XmlElement(name = "DATE")
    var date: String? = null

    @XmlElement(name = "RATE")
    var rATE: String? = null
}
