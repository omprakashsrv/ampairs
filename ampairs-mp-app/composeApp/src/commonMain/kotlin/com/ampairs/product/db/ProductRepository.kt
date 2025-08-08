package com.ampairs.repository

import androidx.paging.PagingSource
import com.ampairs.aws.s3.S3Client
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.flower_core.dbBoundResource
import com.ampairs.common.flower_core.networkResource
import com.ampairs.domain.Unit
import com.ampairs.domain.asUnitConversionModel
import com.ampairs.domain.asUnitDatabaseModel
import com.ampairs.domain.asUnitDomainModel
import com.ampairs.domain.asUnitModel
import com.ampairs.inventory.db.dao.InventoryDao
import com.ampairs.network.model.Response
import com.ampairs.network.model.onError
import com.ampairs.network.model.onSuccess
import com.ampairs.product.api.ProductApi
import com.ampairs.product.api.model.AllProductGroupApiModel
import com.ampairs.product.api.model.ProductApiModel
import com.ampairs.product.api.model.ProductGroupApiModel
import com.ampairs.product.api.model.UnitApiModel
import com.ampairs.product.db.dao.BrandDao
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.product.db.dao.ProductImageDao
import com.ampairs.product.db.dao.SubCategoryDao
import com.ampairs.product.db.dao.TaxCodeDao
import com.ampairs.product.db.dao.UnitConversionDao
import com.ampairs.product.db.dao.UnitDao
import com.ampairs.product.db.entity.BrandEntity
import com.ampairs.product.db.entity.CategoryEntity
import com.ampairs.product.db.entity.GroupEntity
import com.ampairs.product.db.entity.ImageEntity
import com.ampairs.product.db.entity.ProductEntity
import com.ampairs.product.db.entity.SubCategoryEntity
import com.ampairs.product.domain.Group
import com.ampairs.product.domain.Image
import com.ampairs.product.domain.Product
import com.ampairs.product.domain.ProductImage
import com.ampairs.product.domain.TaxCode
import com.ampairs.product.domain.asBrandApiModel
import com.ampairs.product.domain.asBrandDatabaseEntity
import com.ampairs.product.domain.asBrandDatabaseModel
import com.ampairs.product.domain.asBrandDomainModel
import com.ampairs.product.domain.asCategoryApiModel
import com.ampairs.product.domain.asCategoryDatabaseEntity
import com.ampairs.product.domain.asCategoryDatabaseModel
import com.ampairs.product.domain.asCategoryDomainModel
import com.ampairs.product.domain.asCategoryGroupDomainModel
import com.ampairs.product.domain.asDatabaseModel
import com.ampairs.product.domain.asDomainModel
import com.ampairs.product.domain.asGroupApiModel
import com.ampairs.product.domain.asGroupDatabaseEntity
import com.ampairs.product.domain.asGroupDatabaseModel
import com.ampairs.product.domain.asGroupDomainModel
import com.ampairs.product.domain.asImageDomainModel
import com.ampairs.product.domain.asImagesDatabaseEntity
import com.ampairs.product.domain.asInventoryDatabaseModel
import com.ampairs.product.domain.asProductApiModel
import com.ampairs.product.domain.asProductDomainModel
import com.ampairs.product.domain.asProductImageDomainModel
import com.ampairs.product.domain.asSubCategoryApiModel
import com.ampairs.product.domain.asSubCategoryDatabaseEntity
import com.ampairs.product.domain.asSubCategoryDatabaseModel
import com.ampairs.product.domain.asSubCategoryDomainModel
import com.ampairs.product.domain.asTaxCodeModel
import com.ampairs.product.domain.toImageDatabaseModel
import com.ampairs.product.ui.group.GroupType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking

class ProductRepository(
    val productDao: ProductDao,
    val productImageDao: ProductImageDao,
    val groupDao: GroupDao,
    val categoryDao: CategoryDao,
    val subCategoryDao: SubCategoryDao,
    val brandDao: BrandDao,
    val unitDao: UnitDao,
    val unitConversionDao: UnitConversionDao,
    val inventoryDao: InventoryDao,
    val taxDao: TaxCodeDao,
    val productApi: ProductApi,
    val s3Client: S3Client,
) {

    suspend fun syncProducts(onSyncComplete: (Int) -> Unit) {
        val lastUpdated = productDao.getMaxLastUpdated() ?: 0
        productApi.getProducts(lastUpdated, "").onSuccess {
            runBlocking {
                unitDao.updateUnits(this@onSuccess.asUnitModel().toList())
                unitConversionDao.updateUnitConversions(
                    this@onSuccess.asUnitConversionModel().toList()
                )
                taxDao.updateTaxCodes(this@onSuccess.asTaxCodeModel().toList())
                productDao.updateProducts(this@onSuccess.asDatabaseModel())
            }
            onSyncComplete(this.size)
        }.onError {
            println("errorBody = $this")
            onSyncComplete(-1)
        }
    }

    fun getProductPaging(
        searchText: String,
    ): PagingSource<Int, ProductEntity> {
        return productDao.getProductPagingSource(searchText)
    }

    suspend fun getProductImages(ids: List<String>): List<ProductImage> {
        return productImageDao.getProductImagesByProductIds(ids).asProductImageDomainModel()
    }

    fun getProductPagingByGroupAndCategory(
        groupId: String, categoryId: String,
    ): PagingSource<Int, ProductEntity> {
        return productDao.getProductPagingSourceByGroupAndCategory(groupId, categoryId)
    }

    fun getProductPagingByName(
        searchText: String,
    ): PagingSource<Int, ProductEntity> {
        return productDao.getProductPagingSource(searchText)
    }

    fun getGroupResource(groupType: GroupType): Flow<Resource<List<Group>>> {
        return dbBoundResource(
            fetchFromLocal = {
                flow {
                    val groups = getGroups(groupType)
                    groups.forEach {
                        it.image?.url = it.image?.let { it1 ->
                            s3Client.getPreSignedUrl(
                                it1.bucket,
                                it1.objectKey
                            )
                        }
                    }
                    emit(groups)
                }
            },
            shouldMakeNetworkRequest = { true },
            makeNetworkRequest = {
                flow {
                    emit(getAllGroups())
                }
            },
            processNetworkResponse = {

            },
            saveResponseData = {
                runBlocking {
                    saveAllGroups(it)
                }
            },
            onNetworkRequestFailed = { message: String, code: Int ->

            }).flowOn(Dispatchers.IO)
    }

    suspend fun saveAllGroups(it: AllProductGroupApiModel) {
        it.groups?.asGroupDatabaseEntity()?.let { it1 -> groupDao.insertGroups(it1) }
        it.categories?.asCategoryDatabaseEntity()
            ?.let { it1 -> categoryDao.insertCategories(it1) }
        it.subCategories?.asSubCategoryDatabaseEntity()
            ?.let { it1 -> subCategoryDao.insertSubCategories(it1) }
        it.brands?.asBrandDatabaseEntity()?.let { it1 -> brandDao.insertBrands(it1) }
        val images = mutableListOf<ImageEntity>()
        it.groups?.asImagesDatabaseEntity()?.let { it1 -> images.addAll(it1) }
        it.categories?.asImagesDatabaseEntity()?.let { it1 -> images.addAll(it1) }
        it.subCategories?.asImagesDatabaseEntity()?.let { it1 -> images.addAll(it1) }
        it.brands?.asImagesDatabaseEntity()?.let { it1 -> images.addAll(it1) }
        productDao.insertImages(images)
    }

    suspend fun getAllGroups(): Response<AllProductGroupApiModel> {
        return productApi.getAllGroups()
    }

    suspend fun getGroups(groupType: GroupType): List<Group> {
        val groups = when (groupType) {
            GroupType.GROUP -> groupDao.getGroups().asGroupDomainModel()
            GroupType.CATEGORY -> categoryDao.getCategories().asCategoryDomainModel()
            GroupType.SUBCATEGORY -> subCategoryDao.getSubCategories().asSubCategoryDomainModel()
            GroupType.BRAND -> brandDao.getBrands().asBrandDomainModel()
        }
        return groups
    }

    suspend fun getGroup(id: String): Group? {
        return groupDao.groupById(id)?.asGroupDomainModel()
    }

    fun getCategoryResource(groupId: String): Flow<Resource<List<Group>>> {
        return dbBoundResource(
            fetchFromLocal = {
                flow {
                    val categoryIds = productDao.productCategories(groupId)
                    val categories = categoryDao.getCategoriesByIds(categoryIds)
                    emit(categories.asCategoryGroupDomainModel())
                }
            },
            shouldMakeNetworkRequest = { true },
            makeNetworkRequest = {
                flow {
                    emit(productApi.getProducts(null, groupId))
                }
            },
            processNetworkResponse = {

            },
            saveResponseData = {
                runBlocking {
                    val products = it
                    unitDao.updateUnits(products.asUnitModel().toList())
                    unitConversionDao.updateUnitConversions(
                        products.asUnitConversionModel().toList()
                    )
                    taxDao.updateTaxCodes(products.asTaxCodeModel().toList())
                    productDao.updateProducts(products.asDatabaseModel())
                    inventoryDao.updateInventoryList(products.asInventoryDatabaseModel())
                }
            },
            onNetworkRequestFailed = { message: String, code: Int ->

            }).flowOn(Dispatchers.IO)
    }

    suspend fun getProduct(id: String): Product? {
        return productDao.productById(id)?.asDomainModel()
    }

    suspend fun getProducts(): List<Product> {
        return productDao.getProducts().asProductDomainModel()
    }

    suspend fun updateProduct(product: Product) {
        productDao.insert(product.asDatabaseModel())
        val products = productDao.unSyncedProducts()
        val updatedProducts = updateProducts(products.asProductApiModel())
        updatedProducts.data?.map {
            it.lastUpdated = 0
            it
        }?.asDatabaseModel()?.let {
            productDao.updateProducts(it)
        }
    }

    suspend fun updateProducts(products: List<ProductApiModel>): Response<List<ProductApiModel>> {
        return productApi.updateProducts(products)
    }

    suspend fun getTaxCode(taxCode: String): TaxCode? {
        return taxDao.findByCode(taxCode)?.asDomainModel()
    }

    suspend fun uploadImage(fileName: String, file: ByteArray, path: String): Image? {
        val imageApiModel = productApi.uploadImage(fileName, file, path).data
        val image = imageApiModel?.asImageDomainModel()
        image?.url = image?.let { it1 -> s3Client.getPreSignedUrl(it1.bucket, it1.objectKey) }
        return image
    }

    suspend fun saveGroups(groups: List<Group>, groupType: GroupType) {
        val images = groups.filter { it.image != null }.map { it.image!!.toImageDatabaseModel() }
        productDao.insertImages(images)
        when (groupType) {
            GroupType.GROUP -> groupDao.insertGroups(groups.asGroupDatabaseModel())
            GroupType.CATEGORY -> categoryDao.insertCategories(groups.asCategoryDatabaseModel())
            GroupType.SUBCATEGORY -> subCategoryDao.insertSubCategories(groups.asSubCategoryDatabaseModel())
            GroupType.BRAND -> brandDao.insertBrands(groups.asBrandDatabaseModel())
        }
    }

    suspend fun updateGroups(groupType: GroupType) {
        when (groupType) {
            GroupType.GROUP -> {
                val groups: List<GroupEntity> = groupDao.unSyncedGroups()
                val updatedGroups = updateGroups(groups.asGroupApiModel()).data
                updatedGroups?.asGroupDatabaseEntity()?.let { groupDao.insertGroups(it) }
            }

            GroupType.CATEGORY -> {
                val groups: List<CategoryEntity> = categoryDao.unSyncedCategories()
                val updatedGroups = updateCategories(groups.asCategoryApiModel()).data
                updatedGroups?.asCategoryDatabaseEntity()?.let { categoryDao.insertCategories(it) }
            }

            GroupType.SUBCATEGORY -> {
                val groups: List<SubCategoryEntity> = subCategoryDao.unSyncedSubCategories()
                val updatedGroups =
                    productApi.updateSubCategories(groups.asSubCategoryApiModel()).data
                updatedGroups?.asSubCategoryDatabaseEntity()
                    ?.let { subCategoryDao.insertSubCategories(it) }
            }

            GroupType.BRAND -> {
                val groups: List<BrandEntity> = brandDao.unSyncedBrands()
                val updatedGroups = productApi.updateBrands(groups.asBrandApiModel()).data
                updatedGroups?.asBrandDatabaseEntity()?.let { brandDao.insertBrands(it) }
            }
        }
    }


    fun getProductResource(): Flow<Resource<List<ProductApiModel>>> {
        return networkResource(
            shouldMakeNetworkRequest = { true },
            makeNetworkRequest = {
                val sharedFlow = MutableSharedFlow<Response<List<ProductApiModel>>>(replay = 10)
                var fetchSize = 1000
                while (fetchSize == 1000) {
                    val lastUpdated = productDao.getMaxLastUpdated() ?: 0
                    val products = productApi.getProducts(lastUpdated, "")
                    val apiModels = products.data
                    apiModels?.asDatabaseModel()?.let {
                        productDao.updateProducts(it)
                    }
                    apiModels?.asInventoryDatabaseModel()?.let {
                        inventoryDao.updateInventoryList(it)
                    }
                    fetchSize = apiModels?.size ?: 0
                    sharedFlow.emit(products)
                }
                sharedFlow
            },
            processNetworkResponse = {

            }).flowOn(Dispatchers.IO)
    }

    suspend fun updateUnits(units: List<UnitApiModel>): Response<List<UnitApiModel>> {
        return productApi.updateUnits(units)
    }

    suspend fun getUnits(): Response<List<UnitApiModel>> {
        return productApi.getUnits()
    }

    suspend fun updateGroups(groups: List<ProductGroupApiModel>): Response<List<ProductGroupApiModel>> {
        return productApi.updateGroups(groups)
    }

    suspend fun updateCategories(categories: List<ProductGroupApiModel>): Response<List<ProductGroupApiModel>> {
        return productApi.updateCategories(categories)
    }

    suspend fun getAllUnits(): List<Unit> {
        return unitDao.getAllUnits().asUnitDomainModel()
    }

    suspend fun saveUnits(units: List<UnitApiModel>) {
        unitDao.updateUnits(units.asUnitDatabaseModel())
    }

}