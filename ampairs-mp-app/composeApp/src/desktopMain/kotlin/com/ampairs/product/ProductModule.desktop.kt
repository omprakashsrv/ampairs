package com.ampairs.product

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.product.db.ProductRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val productPlatformModule: Module = module {
    single<ProductRoomDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createDatabase(
            klass = ProductRoomDatabase::class,
            moduleName = "product"
        )
    }
}