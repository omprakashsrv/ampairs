package com.ampairs.product

import com.ampairs.product.api.ProductApi
import com.ampairs.product.api.ProductApiImpl
import com.ampairs.product.db.ProductRoomDatabase
import com.ampairs.product.db.TaxDaoAdapter
import com.ampairs.product.db.TaxRepository
import com.ampairs.product.ui.group.ProductGroupEditViewModel
import com.ampairs.product.ui.group.ProductGroupViewModel
import com.ampairs.product.ui.group.ProductSearchViewModel
import com.ampairs.product.ui.product.ProductCategoryViewModel
import com.ampairs.product.ui.product.ProductListViewModel
import com.ampairs.product.ui.product.ProductViewModel
import com.ampairs.product.ui.tax.tax_info.TaxInfoViewModel
import com.ampairs.product.ui.tax.tax_info.TaxInfosViewModel
import com.ampairs.repository.ProductRepository
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module


val productModule: Module = module {
    single { ProductApiImpl(get(), get()) } bind (ProductApi::class)
    // Database is provided by platform-specific modules
    single { get<ProductRoomDatabase>().productDao() }
    single { get<ProductRoomDatabase>().groupDao() }
    single { get<ProductRoomDatabase>().taxCodeDao() }
    single { get<ProductRoomDatabase>().taxInfoDao() }
    single { get<ProductRoomDatabase>().unitDao() }
    single<TaxDaoAdapter> { TaxDaoAdapter(get(), get()) }
    single<TaxRepository> { TaxRepository(get(), get()) }
    single {
        ProductRepository(
            get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get()
        )
    }

    // Direct ViewModel injection
    factory { ProductViewModel() }
    factory { TaxInfosViewModel(get()) }
    factory { ProductSearchViewModel(get(), get(), get()) }
    factory { ProductListViewModel(get()) }
    factory { ProductGroupViewModel(get()) }
    factory { ProductGroupEditViewModel(get()) }
    factory { ProductCategoryViewModel(get(), get(), get(), get()) }
    factory { (taxId: String) -> TaxInfoViewModel(taxId, get()) }
}

fun productModule() = productModule