package com.ampairs.customer.di

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.customer.data.db.CustomerDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val customerPlatformModule = module {
    single<CustomerDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createAndroidDatabase(
            klass = CustomerDatabase::class,
            context = androidContext(),
            queryDispatcher = Dispatchers.IO,
            moduleName = "customer"
        )
    }

}