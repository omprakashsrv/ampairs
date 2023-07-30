package com.ampairs.tally.task

import com.ampairs.api.product.model.ProductCategoryApiModel
import com.ampairs.api.product.model.ProductGroupApiModel
import com.ampairs.network.product.ProductApi
import com.ampairs.network.product.model.TaxCodeApiModel
import com.ampairs.product.domain.dto.UnitApiModel
import com.ampairs.tally.model.Type
import com.ampairs.tally.model.dto.*
import com.ampairs.tally.model.toTallyXML
import com.ampairs.tally.service.TallyClient
import com.skydoves.sandwich.onSuccess
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ProductSyncTask @Autowired constructor(val tallyClient: TallyClient, val productApi: ProductApi) {

    @Scheduled(fixedDelay = 2 * 10 * 1000)
    fun syncUnits() {
        runBlocking {
            val tallyUnits = tallyClient.post(Type.UNIT.toTallyXML())
            val unitResponse = tallyUnits?.body?.data?.collection?.units?.toUnits()?.let { productApi.updateUnits(it) }
            var units: List<UnitApiModel>? = null
            unitResponse?.onSuccess {
                units = this.data.response
            }
            if (units == null) return@runBlocking
            print("Units updated")
            val tallyStockGroups = tallyClient.post(Type.STOCK_GROUP.toTallyXML())
            val groupsResponse = tallyStockGroups?.body?.data?.collection?.stockGroups?.toStockGroups()
                ?.let { productApi.updateProductGroups(it) }
            var groups: List<ProductGroupApiModel>? = null
            groupsResponse?.onSuccess {
                groups = this.data.response
            }
            if (groups == null) return@runBlocking
            print("Stock Group updated")
            val tallyStockCategories = tallyClient.post(Type.STOCK_CATEGORY.toTallyXML())
            val categoriesApiResponse =
                tallyStockCategories?.body?.data?.collection?.stockCategories?.toStockCategories()
                    ?.let { productApi.updateProductCategories(it) }
            var categories: List<ProductCategoryApiModel>? = null
            categoriesApiResponse?.onSuccess {
                categories = this.data.response
            }
            if (categories == null) return@runBlocking
            print("Stock Categories updated")

            val tallyStockItems = tallyClient.post(Type.STOCK_ITEM.toTallyXML())

            var taxCodes: List<TaxCodeApiModel>? = null
            val taxCodesResponse = tallyStockItems?.body?.data?.collection?.stockItems?.toTaxCodes()
                ?.let { productApi.updateTaxCodes(it) }
            taxCodesResponse?.onSuccess {
                taxCodes = this.data.response
            }
            if (taxCodes == null) return@runBlocking

            val productsResponse =
                tallyStockItems?.body?.data?.collection?.stockItems?.toStockItems(units!!, groups!!, categories!!)
                    ?.let {
                        productApi.updateProducts(it)
                    }
            productsResponse?.onSuccess {
                println("Product Master Synced")
            }
        }
    }
}

