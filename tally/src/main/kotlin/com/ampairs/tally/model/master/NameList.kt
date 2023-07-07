package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
class NameList {
    @XmlElement(name = "NAME.LIST", type = Name::class)
    var nameList: List<Name>? = null
}
