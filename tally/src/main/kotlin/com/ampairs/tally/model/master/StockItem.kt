package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class StockItem(
    @XmlAttribute(name = "NAME")
    var name: String? = null,

    @XmlAttribute(name = "RESERVEDNAME")
    var reservedName: String? = null,

    @field:XmlElement(name = "GUID")
    var guid: String? = null,

    @field:XmlElement(name = "PARENT")
    var parent: String? = null,

    @field:XmlElement(name = "CATEGORY")
    var category: String? = null,

    @field:XmlElement(name = "GSTAPPLICABLE")
    var gstApplicable: String? = null,

    @field:XmlElement(name = "TCSAPPLICABLE")
    var tcsApplicable: String? = null,

    @field:XmlElement(name = "TCSCATEGORY")
    var tcsCategory: String? = null,

    @field:XmlElement(name = "GSTTYPEOFSUPPLY")
    var gstTypeOfSupply: String? = null,

    @field:XmlElement(name = "BASEUNITS")
    var baseUnits: String? = null,

    @field:XmlElement(name = "ADDITIONALUNITS")
    var additionalUnits: String? = null,

    @field:XmlElement(name = "GSTREPUOM")
    var gstRepUOM: String? = null,

    @field:XmlElement(name = "DENOMINATOR")
    var denominator: String? = null,

    @field:XmlElement(name = "CONVERSION")
    var conversion: String? = null,

    @field:XmlElement(name = "STANDARDCOSTLIST.LIST", type = StandardCost::class)
    var standardCost: StandardCost? = null,

    @field:XmlElement(name = "STANDARDPRICELIST.LIST", type = StandardPrice::class)
    var standardPrice: StandardPrice? = null,

    @field:XmlElement(name = "GSTDETAILS.LIST", type = GSTClassificationDetail::class)
    var gstClassificationDetailList: List<GSTClassificationDetail>? = null,

    @field:XmlElement(name = "TCSCATEGORYDETAILS.LIST", type = TCSCategoryDetail::class)
    var tcsCategoryDetailList: List<TCSCategoryDetail>? = null,

    @field:XmlElement(name = "NAME.LIST", type = Name::class)
    var nameList: List<Name>? = null,
)
