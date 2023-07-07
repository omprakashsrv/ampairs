package com.ampairs.tally.model

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
data class StaticVariables(
    @XmlElement(name = "SVCURRENTCOMPANY")
    var svCurrenCompany: String
)