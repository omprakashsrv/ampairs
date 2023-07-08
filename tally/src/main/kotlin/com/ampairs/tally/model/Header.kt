package com.ampairs.tally.model

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class Header(
    @field:XmlElement(name = "TALLYREQUEST")
    var tallyString: String? = null
)
