package com.ampairs.product.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.postMultiPart
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
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import io.ktor.http.content.PartData

const val PRODUCT_ENDPOINT = "http://localhost:8080"

class ProductApiImpl(engine: HttpClientEngine, tokenRepository: TokenRepository) : ProductApi {

    private val client = httpClient(engine, tokenRepository)
    override suspend fun getProducts(
        lastUpdated: Long?,
        groupId: String?,
    ): Response<List<ProductApiModel>> {
        return get(
            client,
            PRODUCT_ENDPOINT + "/product/v1",
            buildMap {
                lastUpdated?.let { put("last_updated", it) }
                groupId?.let { put("group_id", it) }
            })
    }

    override suspend fun getGroups(groupType: GroupType): Response<List<ProductGroupApiModel>> {
        val groups = when (groupType) {
            GroupType.GROUP -> "groups"
            GroupType.CATEGORY -> "categories"
            GroupType.SUBCATEGORY -> "sub_categories"
            GroupType.BRAND -> "brands"
        }
        return get(
            client,
            "$PRODUCT_ENDPOINT/product/v1/$groups"
        )
    }

    override suspend fun getTaxInfos(): Response<List<TaxInfoApiModel>> {
        return get(
            client,
            "$PRODUCT_ENDPOINT/product/v1/tax/tax_infos",
        )
    }

    override suspend fun getTaxCodes(): Response<List<TaxCodeApiModel>> {
        return get(
            client,
            "$PRODUCT_ENDPOINT/product/v1/tax/tax_codes",
        )
    }

    override suspend fun getAllGroups(): Response<AllProductGroupApiModel> {
        return get(
            client,
            "$PRODUCT_ENDPOINT/product/v1/all_groups_category"
        )
    }

    override suspend fun getUnits(): Response<List<UnitApiModel>> {
        return get(
            client,
            "$PRODUCT_ENDPOINT/product/v1/units"
        )
    }

    override suspend fun getProductCategory(groupId: String): Response<ProductCategoryApiModel> {
        return get(
            client,
            "$PRODUCT_ENDPOINT/product/v1/product_category",
            buildMap {
                put("group_id", groupId)
            }
        )
    }

    override suspend fun uploadImage(
        fileName: String,
        file: ByteArray,
        path: String,
    ): Response<ImageApiModel> {
        val parts: List<PartData> = formData {
            append("file", file, Headers.build {
                append(HttpHeaders.ContentType, ContentType.Image.Any)
                append(HttpHeaders.ContentDisposition, "filename=\"${fileName}\"")
            })
            append("path", path.replace(" ", ""))
        }
        return postMultiPart(
            client,
            "$PRODUCT_ENDPOINT/product/v1/upload_image",
            parts
        )
    }

    override suspend fun updateTaxInfos(taxInfos: List<TaxInfoApiModel>): Response<List<TaxInfoApiModel>> {
        return post(
            client,
            "$PRODUCT_ENDPOINT/product/v1/tax/tax_infos",
            taxInfos
        )
    }

    override suspend fun updateTaxCodes(taxCodes: List<TaxCodeApiModel>): Response<List<TaxCodeApiModel>> {
        return post(
            client,
            "$PRODUCT_ENDPOINT/product/v1/tax/tax_codes",
            taxCodes
        )
    }

    override suspend fun updateGroups(groups: List<ProductGroupApiModel>): Response<List<ProductGroupApiModel>> {
        return post(
            client,
            "$PRODUCT_ENDPOINT/product/v1/groups",
            groups
        )
    }

    override suspend fun updateBrands(groups: List<ProductGroupApiModel>): Response<List<ProductGroupApiModel>> {
        return post(
            client,
            "$PRODUCT_ENDPOINT/product/v1/brands",
            groups
        )
    }

    override suspend fun updateCategories(groups: List<ProductGroupApiModel>): Response<List<ProductGroupApiModel>> {
        return post(
            client,
            "$PRODUCT_ENDPOINT/product/v1/categories",
            groups
        )
    }

    override suspend fun updateSubCategories(groups: List<ProductGroupApiModel>): Response<List<ProductGroupApiModel>> {
        return post(
            client,
            "$PRODUCT_ENDPOINT/product/v1/sub_categories",
            groups
        )
    }

    override suspend fun updateProducts(products: List<ProductApiModel>): Response<List<ProductApiModel>> {
        return post(
            client,
            "$PRODUCT_ENDPOINT/product/v1/products",
            products
        )
    }

    override suspend fun updateUnits(units: List<UnitApiModel>): Response<List<UnitApiModel>> {
        return post(
            client,
            "$PRODUCT_ENDPOINT/product/v1/units",
            units
        )
    }
}