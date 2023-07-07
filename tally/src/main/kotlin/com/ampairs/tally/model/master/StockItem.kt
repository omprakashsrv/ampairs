package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
class StockItem {
    @XmlAttribute(name = "NAME")
    var name = ""

    @XmlAttribute(name = "RESERVEDNAME")
    var reservedName = ""

    @XmlElement(name = "PARENT")
    var parent: String? = null

    @XmlElement(name = "GSTAPPLICABLE")
    var gstApplicable: String? = null

    @XmlElement(name = "TCSAPPLICABLE")
    var tcsApplicable: String? = null

    @XmlElement(name = "TCSCATEGORY")
    var tcsCategory: String? = null

    @XmlElement(name = "GSTTYPEOFSUPPLY")
    var gstTypeOfSupply: String? = null

    @XmlElement(name = "BASEUNITS")
    var baseUnits: String? = null

    @XmlElement(name = "ADDITIONALUNITS")
    var additionalUnits: String? = null

    @XmlElement(name = "GSTREPUOM")
    var gstRepUOM: String? = null

    @XmlElement(name = "DENOMINATOR")
    var denominator: String? = null

    @XmlElement(name = "CONVERSION")
    var conversion: String? = null

    @XmlElement(name = "STANDARDCOSTLIST.LIST", type = StandardCost::class)
    var standardCostList: StandardCost? = null

    @XmlElement(name = "GSTDETAILS.LIST", type = GSTClassificationDetail::class)
    var gstClassificationDetailList: List<GSTClassificationDetail>? = null

    @XmlElement(name = "TCSCATEGORYDETAILS.LIST", type = TCSCategoryDetail::class)
    var tcsCategoryDetailList: List<TCSCategoryDetail>? = null

    @XmlElement(name = "NAME.LIST", type = Name::class)
    var nameList: List<Name>? = null
}
