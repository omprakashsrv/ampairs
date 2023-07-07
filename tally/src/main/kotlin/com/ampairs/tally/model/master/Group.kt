package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
class Group {
    @XmlAttribute(name = "NAME")
    var name = ""

    @XmlAttribute(name = "RESERVEDNAME")
    var reservedName = ""

    @XmlElement(name = "PARENT")
    var parent: String? = null

    @XmlElement(name = "ISBILLWISEON")
    var isBillwiseOn: String? = null

    @XmlElement(name = "ASORIGINAL")
    var asOriginal: String? = null

    @XmlElement(name = "ISSUBLEDGER")
    var isSubLedger: String? = null

    @XmlElement(name = "TRACKNEGATIVEBALANCES")
    var trackNegativeBalances: String? = null

    @XmlElement(name = "LANGUAGENAME.LIST", type = NameList::class)
    var nameList: List<NameList>? = null

    @XmlElement(name = "ADDITIONALNAME")
    var additionalName: String? = null
}
