package com.ampairs.customer.di

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.customer.data.db.CustomerDatabase
import com.ampairs.customer.data.repository.IosFileManager
import com.ampairs.customer.data.repository.PlatformFileManager
import org.koin.dsl.bind
import org.koin.dsl.module

actual val customerPlatformModule = module {
    single<CustomerDatabase> {
        get<WorkspaceAwareDatabaseFactory>().createDatabase(
            klass = CustomerDatabase::class,
            moduleName = "customer"
        )
    }

    single { IosFileManager() } bind PlatformFileManager::class
}