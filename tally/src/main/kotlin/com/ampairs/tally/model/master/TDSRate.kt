package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
class TDSRate {
    @XmlAttribute(name = "NAME")
    var name = ""

    @XmlAttribute(name = "RESERVEDNAME")
    var reservedName = ""

    @XmlElement(name = "ASORIGINAL")
    var asOriginal: String? = null

    @XmlElement(name = "ISACTIVE")
    var isActive: String? = null

    @XmlElement(name = "ISFORTCS")
    var isForTCS: String? = null

    @XmlElement(name = "NAME.LIST", type = Name::class)
    var nameList: List<Name>? = null

    @XmlElement(name = "CATEGORYDETAILS.LIST", type = CategoryDetail::class)
    var categoryDetails: List<CategoryDetail>? = null
}
