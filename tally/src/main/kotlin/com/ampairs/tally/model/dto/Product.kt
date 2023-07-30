package com.ampairs.tally.model.dto

import com.ampairs.api.product.model.ProductCategoryApiModel
import com.ampairs.api.product.model.ProductGroupApiModel
import com.ampairs.network.product.model.ProductUpdateApiModel
import com.ampairs.network.product.model.TaxCodeApiModel
import com.ampairs.product.domain.dto.UnitApiModel
import com.ampairs.tally.model.master.StockCategory
import com.ampairs.tally.model.master.StockGroup
import com.ampairs.tally.model.master.StockItem
import com.ampairs.tally.model.master.Unit
import java.text.SimpleDateFormat
import java.util.*

fun List<Unit>.toUnits(): List<UnitApiModel> {
    return map {
        UnitApiModel(
            id = "",
            name = it.unitName ?: "",
            decimalPlaces = it.decimalPlaces?.trim()?.toInt() ?: 2,
            refId = it.guid ?: "",
            shortName = it.name ?: ""
        )
    }
}

fun List<StockGroup>.toStockGroups(): List<ProductGroupApiModel> {
    return map {
        ProductGroupApiModel(
            id = "",
            name = it.name ?: "",
            refId = it.guid ?: "",
        )
    }
}

fun List<StockCategory>.toStockCategories(): List<ProductCategoryApiModel> {
    return map {
        ProductCategoryApiModel(
            id = "",
            name = it.name ?: "",
            refId = it.guid ?: "",
        )
    }
}

fun List<StockItem>.toTaxCodes(): List<TaxCodeApiModel> {
    val taxCodeList = mutableListOf<TaxCodeApiModel>()
    forEach { stockItem ->
        stockItem.gstDetailList?.forEach { gstDetails ->
            val rateDetailsList = gstDetails.stateWiseDetailsList?.get(0)?.rateDetailsList
            val effectiveFrom =
                Date(SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).parse(gstDetails.applicableFrom).time)
            val find = taxCodeList.find { it1 ->
                it1.code == gstDetails.hsnCode && it1.effectiveFrom == effectiveFrom
            }
            if (find == null) {
                taxCodeList.add(
                    TaxCodeApiModel(
                        id = "",
                        code = gstDetails.hsnCode ?: "",
                        type = "HSN",
                        description = "",
                        cess = rateDetailsList?.find { it.gstRateDutyHead == "Cess" }?.gstRate?.toDouble() ?: 0.0,
                        cgst = rateDetailsList?.find { it.gstRateDutyHead == "Central Tax" }?.gstRate?.toDouble()
                            ?: 0.0,
                        sgst = rateDetailsList?.find { it.gstRateDutyHead == "State Tax" }?.gstRate?.toDouble() ?: 0.0,
                        igst = rateDetailsList?.find { it.gstRateDutyHead == "Integrated Tax" }?.gstRate?.toDouble()
                            ?: 0.0,
                        effectiveFrom = effectiveFrom
                    )
                )
            }
        }
    }
    return taxCodeList.toList()
}

fun List<StockItem>.toStockItems(
    units: List<UnitApiModel>,
    groups: List<ProductGroupApiModel>,
    categories: List<ProductCategoryApiModel>,
): List<ProductUpdateApiModel> {
    return map {
        ProductUpdateApiModel(
            id = "",
            name = it.name ?: "",
            categoryId = categories.find { category ->
                it.category == category.name
            }?.id,
            groupId = groups.find { group ->
                it.parent == group.name
            }?.id,
            baseUnitId = units.find { unit ->
                it.baseUnits == unit.name
            }?.id,
            refId = it.guid ?: "",
            active = true,
            mrp = 0.0,
            taxCode = it.gstDetailList?.get(0)?.hsnCode ?: "",
            sellingPrice = it.standardPrice?.rate?.split("/")?.get(0)?.toDouble() ?: 0.0,
            dp = it.standardCost?.rate?.split("/")?.get(0)?.toDouble() ?: 0.0,
            unitConversions = null
        )
    }
}