package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class Group(
    @XmlAttribute(name = "NAME")
    var name: String? = null,

    @XmlAttribute(name = "RESERVEDNAME")
    var reservedName: String? = null,

    @field:XmlElement(name = "PARENT")
    var parent: String? = null,

    @field:XmlElement(name = "ISBILLWISEON")
    var isBillwiseOn: String? = null,

    @field:XmlElement(name = "ASORIGINAL")
    var asOriginal: String? = null,

    @field:XmlElement(name = "ISSUBLEDGER")
    var isSubLedger: String? = null,

    @field:XmlElement(name = "TRACKNEGATIVEBALANCES")
    var trackNegativeBalances: String? = null,

    @field:XmlElement(name = "LANGUAGENAME.LIST", type = NameList::class)
    var nameList: List<NameList>? = null,

    @field:XmlElement(name = "ADDITIONALNAME")
    var additionalName: String? = null,
)
