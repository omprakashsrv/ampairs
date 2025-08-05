package com.ampairs.product.ui.product

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ampairs.product.domain.Group
import com.ampairs.product.domain.Product
import com.ampairs.product.domain.TaxCode

class ProductState(val product: Product) {
    var name by mutableStateOf(this.product.name)
    var code by mutableStateOf(this.product.code)
    var mrp by mutableStateOf(this.product.mrp)
    var dp by mutableStateOf(this.product.dp)
    var sellingPrice by mutableStateOf(this.product.sellingPrice)
    var mrpFraction by mutableStateOf(product.mrp.toInt().toDouble() != product.mrp)
    var dpFraction by mutableStateOf(product.dp.toInt().toDouble() != product.dp)
    var sellingPriceFraction by mutableStateOf(
        product.sellingPrice.toInt().toDouble() != product.sellingPrice
    )
    var baseUnit by mutableStateOf(this.product.baseUnit)

    var taxCodeExpanded by mutableStateOf(false)
    var groupExpanded by mutableStateOf(false)
    var categoryExpanded by mutableStateOf(false)
    var subCategoryExpanded by mutableStateOf(false)
    var brandExpanded by mutableStateOf(false)

    var taxCode: TaxCode? = null
    var group: Group? = null
    var category: Group? = null
    var subCategory: Group? = null
    var brand: Group? = null
}

fun ProductState.toDomainModel(): Product {
    this.product.name = name
    this.product.code = code
    this.product.mrp = mrp
    this.product.dp = dp
    this.product.sellingPrice = sellingPrice
    this.product.baseUnit = baseUnit
    this.product.brandId = brand?.id ?: ""
    this.product.groupId = group?.id ?: ""
    this.product.categoryId = category?.id ?: ""
    this.product.subCategoryId = subCategory?.id ?: ""
    this.product.code = taxCode?.code ?: ""
    this.product.taxInfos = taxCode?.taxInfos
    return this.product
}