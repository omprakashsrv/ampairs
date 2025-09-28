package com.ampairs.customer.di

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.customer.data.db.CustomerDatabase
import org.koin.dsl.module

actual val customerPlatformModule = module {
    single<CustomerDatabase> {
        get<WorkspaceAwareDatabaseFactory>().createDatabase(
            klass = CustomerDatabase::class,
            moduleName = "customer"
        )
    }

}