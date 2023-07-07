package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

/**
 * Created by paripranu on 06/13/18.
 */
@XmlAccessorType(XmlAccessType.NONE)
class GSTClassification {
    @XmlAttribute(name = "NAME")
    var name = ""

    @XmlAttribute(name = "RESERVEDNAME")
    var reservedName = ""

    @XmlElement(name = "ASORIGINAL")
    var asOriginal: String? = null

    @XmlElement(name = "ISACTIVE")
    var isActive: String? = null

    @XmlElement(name = "NAME.LIST", type = Name::class)
    var nameList: List<Name>? = null

    @XmlElement(name = "GSTDETAILS.LIST", type = GSTDetail::class)
    var gstDetailsList: List<GSTDetail>? = null
}
