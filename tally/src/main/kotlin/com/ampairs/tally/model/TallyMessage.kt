package com.ampairs.tally.model

import com.ampairs.tally.model.master.*
import com.ampairs.tally.model.master.Unit
import com.ampairs.tally.model.voucher.Voucher
import jakarta.xml.bind.annotation.*
import kotlin.String
import kotlin.arrayOf

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    propOrder = arrayOf(
        "unit",
        "stockGroup",
        "gstClassification",
        "group",
        "stockItem",
        "tdsRate",
        "ledger",
        "voucher"
    )
)
data class TallyMessage(
    @XmlAttribute(name = "xmlns:UDF")
    val xmlsUdf: String? = null,

    @field:XmlElement(name = "VOUCHER", type = Voucher::class)
    val voucher: Voucher? = null,

    @field:XmlElement(name = "LEDGER", type = Ledger::class)
    val ledger: Ledger? = null,

    @field:XmlElement(name = "UNIT", type = Unit::class)
    val unit: Unit? = null,

    @field:XmlElement(name = "STOCKGROUP", type = StockGroup::class)
    val stockGroup: StockGroup? = null,

    @field:XmlElement(name = "STOCKITEM", type = StockItem::class)
    val stockItem: StockItem? = null,

    @field:XmlElement(name = "GSTCLASSIFICATION", type = GSTClassification::class)
    val gstClassification: GSTClassification? = null,

    @field:XmlElement(name = "TDSRATE", type = TDSRate::class)
    val tdsRate: TDSRate? = null,

    @field:XmlElement(name = "GROUP", type = Group::class)
    val group: Group? = null,

    )
