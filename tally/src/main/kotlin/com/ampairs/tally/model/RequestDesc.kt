package com.ampairs.tally.model

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
data class RequestDesc(
    @XmlElement(name = "REPORTNAME")
    var reportName: String,

    @XmlElement(name = "STATICVARIABLES", type = StaticVariables::class)
    var staticVariables: StaticVariables
)
