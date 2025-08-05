package com.ampairs.product.api

import com.ampairs.network.model.Response
import com.ampairs.product.api.model.AllProductGroupApiModel
import com.ampairs.product.api.model.ImageApiModel
import com.ampairs.product.api.model.ProductApiModel
import com.ampairs.product.api.model.ProductCategoryApiModel
import com.ampairs.product.api.model.ProductGroupApiModel
import com.ampairs.product.api.model.TaxCodeApiModel
import com.ampairs.product.api.model.TaxInfoApiModel
import com.ampairs.product.api.model.UnitApiModel
import com.ampairs.product.ui.group.GroupType

interface ProductApi {

    //    @GET("/product/v1")
    suspend fun getProducts(lastUpdated: Long?, groupId: String?): Response<List<ProductApiModel>>
    suspend fun getGroups(groupType: GroupType): Response<List<ProductGroupApiModel>>
    suspend fun getTaxInfos(): Response<List<TaxInfoApiModel>>
    suspend fun getTaxCodes(): Response<List<TaxCodeApiModel>>
    suspend fun getAllGroups(): Response<AllProductGroupApiModel>
    suspend fun getUnits(): Response<List<UnitApiModel>>
    suspend fun getProductCategory(groupId: String): Response<ProductCategoryApiModel>
    suspend fun uploadImage(
        fileName: String,
        file: ByteArray,
        path: String,
    ): Response<ImageApiModel>

    suspend fun updateTaxInfos(taxInfos: List<TaxInfoApiModel>): Response<List<TaxInfoApiModel>>
    suspend fun updateTaxCodes(taxCodes: List<TaxCodeApiModel>): Response<List<TaxCodeApiModel>>
    suspend fun updateGroups(groups: List<ProductGroupApiModel>): Response<List<ProductGroupApiModel>>
    suspend fun updateBrands(groups: List<ProductGroupApiModel>): Response<List<ProductGroupApiModel>>
    suspend fun updateCategories(groups: List<ProductGroupApiModel>): Response<List<ProductGroupApiModel>>
    suspend fun updateSubCategories(groups: List<ProductGroupApiModel>): Response<List<ProductGroupApiModel>>
    suspend fun updateProducts(products: List<ProductApiModel>): Response<List<ProductApiModel>>
    suspend fun updateUnits(units: List<UnitApiModel>): Response<List<UnitApiModel>>


}