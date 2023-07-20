package com.ampairs.tally.model.export

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)

data class Header(
    @field:XmlElement(name = "VERSION")
    var version: String = "1",
    @field:XmlElement(name = "TALLYREQUEST")
    var tallyRequest: String = "EXPORT",
    @field:XmlElement(name = "TYPE")
    var type: String = "COLLECTION",
    @field:XmlElement(name = "ID")
    var id: String = "",
)