package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
class StockGroup {
    @XmlAttribute(name = "NAME")
    var name = ""

    @XmlAttribute(name = "RESERVEDNAME")
    var reservedName = ""

    @XmlElement(name = "NAME.LIST", type = Name::class)
    var nameList: List<Name>? = null

    @XmlElement(name = "PARENT")
    var parent = ""
}
