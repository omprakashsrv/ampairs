package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
data class Unit(
    @XmlAttribute(name = "NAME")
    var name: String,

    @XmlAttribute(name = "RESERVEDNAME")
    var reservedName: String,

    @XmlElement(name = "NAME")
    var unitName: String?,

    @XmlElement(name = "GSTREPUOM")
    var gstRepUOM: String?,

    @XmlElement(name = "DECIMALPLACES")
    var decimalPlaces: String?,

    @XmlElement(name = "ISSIMPLEUNIT")
    var isSimpleUnit: String?,
)
