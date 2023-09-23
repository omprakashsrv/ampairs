package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.Product
import com.ampairs.product.domain.model.Unit
import com.ampairs.product.domain.model.group.ProductCategory
import com.ampairs.product.domain.model.group.ProductGroup
import com.ampairs.tally.model.master.StockCategory
import com.ampairs.tally.model.master.StockGroup
import com.ampairs.tally.model.master.StockItem

fun com.ampairs.tally.model.master.Unit.asDatabaseModel(): Unit {
    val unit = Unit()
    unit.refId = this.guid
    unit.name = this.name ?: ""
    unit.shortName = this.reservedName ?: ""
    unit.decimalPlaces = this.decimalPlaces?.trim()?.toInt() ?: 0
    return unit
}

fun StockGroup.asDatabaseModel(): ProductGroup {
    val productGroup = ProductGroup()
    productGroup.refId = this.guid
    productGroup.name = this.name ?: ""
    return productGroup
}

fun StockCategory.asDatabaseModel(): ProductCategory {
    val productCategory = ProductCategory()
    productCategory.refId = this.guid
    productCategory.name = this.name ?: ""
    return productCategory
}

fun StockItem.asDatabaseModel(): Product {
    val product = Product()
    product.refId = this.guid
    product.name = this.name ?: ""
    product.taxCode = this.gstDetailList?.get(0)?.hsnCode ?: ""
    product.mrp = 0.0
    product.sellingPrice = this.standardPrice?.rate?.split("/")?.get(0)?.toDouble() ?: 0.0
    product.dp = this.standardCost?.rate?.split("/")?.get(0)?.toDouble() ?: 0.0
    return product
}