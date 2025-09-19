package com.ampairs.product

import com.ampairs.product.data.api.ProductApi
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.product.db.ProductRoomDatabase
import com.ampairs.product.data.repository.ProductRepository
import com.ampairs.product.domain.ProductStore
import com.ampairs.product.ui.create.ProductFormViewModel
import com.ampairs.product.ui.details.ProductDetailsViewModel
import com.ampairs.product.ui.list.ProductsListViewModel
import org.koin.core.module.Module
import org.koin.dsl.module


val productModule: Module = module {
    // Store5 based components
    single<ProductDao> { get<ProductRoomDatabase>().productDao() }

    // Placeholder API implementation - to be replaced with actual implementation
    single<ProductApi> {
        object : ProductApi {
            override suspend fun getProducts(workspaceId: String) = Result.failure<List<com.ampairs.product.api.model.ProductApiModel>>(Exception("Not implemented"))
            override suspend fun getProduct(workspaceId: String, productId: String) = Result.failure<com.ampairs.product.api.model.ProductApiModel>(Exception("Not implemented"))
            override suspend fun createProduct(workspaceId: String, product: com.ampairs.product.api.model.ProductApiModel) = Result.failure<com.ampairs.product.api.model.ProductApiModel>(Exception("Not implemented"))
            override suspend fun updateProduct(workspaceId: String, productId: String, product: com.ampairs.product.api.model.ProductApiModel) = Result.failure<com.ampairs.product.api.model.ProductApiModel>(Exception("Not implemented"))
            override suspend fun deleteProduct(workspaceId: String, productId: String) = Result.failure<Unit>(Exception("Not implemented"))
            override suspend fun searchProducts(workspaceId: String, query: String) = Result.failure<List<com.ampairs.product.api.model.ProductApiModel>>(Exception("Not implemented"))
        }
    }

    single<ProductRepository> { ProductRepository(get(), get()) }

    // Product Store for offline-first pattern
    single<ProductStore> { ProductStore(get()) }

    // ViewModels for Store5 pattern
    factory { ProductsListViewModel(get(), get()) }
    factory { (productId: String?) -> ProductFormViewModel(productId, get(), get()) }
    factory { (productId: String) -> ProductDetailsViewModel(productId, get(), get()) }
}

fun productModule() = productModule