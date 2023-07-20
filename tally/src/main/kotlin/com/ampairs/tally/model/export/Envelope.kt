package com.ampairs.tally.model.export

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class Envelope(
    @XmlAttribute(name = "Action")
    val action: String = "",

    @field:XmlElement(name = "HEADER", type = Header::class)
    var header: Header = Header(),

    @field:XmlElement(name = "BODY", type = Body::class)
    var body: Body = Body()
)