package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class StandardCost(
    @field:XmlElement(name = "DATE")
    var date: String? = null,

    @field:XmlElement(name = "RATE")
    var rate: String? = null,
)
