package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.Product
import com.ampairs.product.domain.model.ProductGroup
import com.ampairs.product.domain.model.Unit
import com.ampairs.tally.model.master.StockGroup
import com.ampairs.tally.model.master.StockItem

fun com.ampairs.tally.model.master.Unit.asDatabaseModel(): Unit {
    val unit = Unit()
    unit.name = this.name ?: ""
    unit.shortName = this.reservedName ?: ""
    unit.decimalPlaces = this.decimalPlaces?.toInt() ?: 0
    return unit
}

fun StockGroup.asDatabaseModel(): ProductGroup {
    val productGroup = ProductGroup()
    productGroup.name = this.name ?: ""
    return productGroup
}

fun StockItem.asDatabaseModel(): Product {
    val product = Product()
    product.name = this.name ?: ""
    product.baseUnitId = this.baseUnits ?: ""
    product.mrp = this.standardCost?.rate?.toDouble() ?: 0.0
    return product
}