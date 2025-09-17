package com.ampairs.product

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.product.db.ProductRoomDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val productPlatformModule: Module = module {
    single<ProductRoomDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createAndroidDatabase(
            klass = ProductRoomDatabase::class,
            context = androidContext(),
            queryDispatcher = Dispatchers.IO,
            moduleName = "product"
        )
    }
}