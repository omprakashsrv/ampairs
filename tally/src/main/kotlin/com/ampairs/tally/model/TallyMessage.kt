package com.ampairs.tally.model

import com.ampairs.tally.model.master.*
import com.ampairs.tally.model.master.Unit
import com.ampairs.tally.model.voucher.Voucher
import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement
import kotlin.String

@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(propOrder = {"xmlsUdf", "group","voucher", "ledger", "unit", "stockGroup", "stockItem","gstClassification"})
data class TallyMessage(
    @XmlAttribute(name = "xmlns:UDF")
    var xmlsUdf: String? = null,

    @field:XmlElement(name = "VOUCHER", type = Voucher::class)
    var voucher: Voucher? = null,

    @field:XmlElement(name = "LEDGER", type = Ledger::class)
    var ledger: Ledger? = null,

    @field:XmlElement(name = "UNIT", type = Unit::class)
    var unit: Unit? = null,

    @field:XmlElement(name = "STOCKGROUP", type = StockGroup::class)
    var stockGroup: StockGroup? = null,

    @field:XmlElement(name = "STOCKITEM", type = StockItem::class)
    var stockItem: StockItem? = null,

    @field:XmlElement(name = "GSTCLASSIFICATION", type = GSTClassification::class)
    var gstClassification: GSTClassification? = null,

    @field:XmlElement(name = "TDSRATE", type = TDSRate::class)
    var tdsRate: TDSRate? = null,

    @field:XmlElement(name = "GROUP", type = Group::class)
    var group: Group? = null,
)
