package com.ampairs.tally.model

import com.ampairs.tally.model.master.*
import com.ampairs.tally.model.master.Unit
import com.ampairs.tally.model.voucher.Voucher
import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement
import kotlin.String

@XmlAccessorType(XmlAccessType.NONE) //@XmlType(propOrder = {"xmlsUdf", "group","voucher", "ledger", "unit", "stockGroup", "stockItem","gstClassification"})

data class TallyMessage(
    @XmlAttribute(name = "xmlns:UDF")
    var xmlsUdf: String,

    @XmlElement(name = "VOUCHER", type = Voucher::class)
    var voucher: Voucher?,

    @XmlElement(name = "LEDGER", type = Ledger::class)
    var ledger: Ledger?,

    @XmlElement(name = "UNIT", type = Unit::class)
    var unit: Unit?,

    @XmlElement(name = "STOCKGROUP", type = StockGroup::class)
    var stockGroup: StockGroup?,

    @XmlElement(name = "STOCKITEM", type = StockItem::class)
    var stockItem: StockItem?,

    @XmlElement(name = "GSTCLASSIFICATION", type = GSTClassification::class)
    var gstClassification: GSTClassification?,

    @XmlElement(name = "TDSRATE", type = TDSRate::class)
    var tdsRate: TDSRate?,

    @XmlElement(name = "GROUP", type = Group::class)
    var group: Group?,
)
