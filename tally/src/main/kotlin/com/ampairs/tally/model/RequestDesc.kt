package com.ampairs.tally.model

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class RequestDesc(
    @field:XmlElement(name = "REPORTNAME")
    var reportName: String? = null,

    @field:XmlElement(name = "STATICVARIABLES", type = StaticVariables::class)
    var staticVariables: StaticVariables? = null
)
