package com.ampairs.customer

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.customer.db.CustomerRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val customerPlatformModule: Module = module {
    single<CustomerRoomDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createDatabase(
            klass = CustomerRoomDatabase::class,
            moduleName = "customer"
        )
    }
}