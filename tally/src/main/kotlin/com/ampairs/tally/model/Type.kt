package com.ampairs.tally.model

enum class Type(var type: String, var id: String) {
    UNIT("UNIT", "CUSTOMUNITCOL"),
    STOCK_GROUP("STOCKGROUP", "CUSTOMSTOCKGROUPCOL"),
    STOCK_CATEGORY("STOCKCATEGORY", "CUSTOMSTOCKCATEGORYCOL"),
    STOCK_ITEM("STOCKITEM", "CUSTOMSTOCKITEMCOL"),
    GST_CLASSIFICATION("UNIT", "CUSTOMLEDGERCOL"), TDS_RATE("UNIT", "CUSTOMLEDGERCOL"),
    GROUP("UNIT", "CUSTOMLEDGERCOL"),
    LEDGER("LEDGER", "CUSTOMLEDGERCOL")
}

fun Type.toTallyXML(): TallyXML {
    val tallyXML = TallyXML()
    tallyXML.header.id = this.id
    tallyXML.body.desc.tdl.tdlMessage.collection.name = this.id
    tallyXML.body.desc.tdl.tdlMessage.collection.type = this.type
    return tallyXML
}
