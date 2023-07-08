package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class StockGroup(
    @XmlAttribute(name = "NAME")
    var name: String? = null,

    @XmlAttribute(name = "RESERVEDNAME")
    var reservedName: String? = null,

    @field:XmlElement(name = "NAME.LIST", type = Name::class)
    var nameList: List<Name>? = null,

    @field:XmlElement(name = "PARENT")
    var parent: String? = null,
)
