package com.ampairs.customer

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.customer.db.CustomerRoomDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val customerPlatformModule: Module = module {
    single<CustomerRoomDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createAndroidDatabase(
            klass = CustomerRoomDatabase::class,
            context = androidContext(),
            queryDispatcher = Dispatchers.IO,
            moduleName = "customer"
        )
    }
}