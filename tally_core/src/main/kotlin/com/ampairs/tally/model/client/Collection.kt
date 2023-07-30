package com.ampairs.tally.model.client

import com.ampairs.tally.model.master.*
import com.ampairs.tally.model.master.Unit
import com.ampairs.tally.model.voucher.Voucher
import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement
import kotlin.String


@XmlAccessorType(XmlAccessType.FIELD)
data class Collection(
    @XmlAttribute(name = "ISMODIFY")
    var isModify: String = "No",
    @XmlAttribute(name = "ISFIXED")
    var isFixed: String = "No",
    @XmlAttribute(name = "ISINITIALIZE")
    var isInitialize: String = "No",
    @XmlAttribute(name = "ISOPTION")
    var isOption: String = "No",
    @XmlAttribute(name = "ISINTERNAL")
    var isInternal: String = "No",
    @XmlAttribute(name = "NAME")
    var name: String = "",
    @field:XmlElement(name = "TYPE")
    var type: String = "",
    @field:XmlElement(name = "NATIVEMETHOD")
    var nativeMethod: String = "*",


    @field:XmlElement(name = "VOUCHER", type = Voucher::class)
    val voucher: List<Voucher>? = null,

    @field:XmlElement(name = "LEDGER", type = Ledger::class)
    val ledgers: List<Ledger>? = null,

    @field:XmlElement(name = "UNIT", type = Unit::class)
    val units: List<Unit>? = null,

    @field:XmlElement(name = "STOCKGROUP", type = StockGroup::class)
    val stockGroups: List<StockGroup>? = null,

    @field:XmlElement(name = "STOCKCATEGORY", type = StockCategory::class)
    val stockCategories: List<StockCategory>? = null,

    @field:XmlElement(name = "STOCKITEM", type = StockItem::class)
    val stockItems: List<StockItem>? = null,

    @field:XmlElement(name = "GSTCLASSIFICATION", type = GSTClassification::class)
    val gstClassifications: List<GSTClassification>? = null,

    @field:XmlElement(name = "TDSRATE", type = TDSRate::class)
    val tdsRates: List<TDSRate>? = null,

    @field:XmlElement(name = "GROUP", type = Group::class)
    val groups: List<Group>? = null,
)