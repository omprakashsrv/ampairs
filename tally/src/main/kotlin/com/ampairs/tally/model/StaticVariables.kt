package com.ampairs.tally.model

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class StaticVariables(
    @field:XmlElement(name = "SVCURRENTCOMPANY")
    var svCurrenCompany: String? = null
)