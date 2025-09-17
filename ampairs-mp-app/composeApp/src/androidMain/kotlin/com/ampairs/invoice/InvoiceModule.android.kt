package com.ampairs.invoice

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.invoice.db.InvoiceRoomDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val invoicePlatformModule: Module = module {
    single<InvoiceRoomDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createAndroidDatabase(
            klass = InvoiceRoomDatabase::class,
            context = androidContext(),
            queryDispatcher = Dispatchers.IO,
            moduleName = "invoice"
        )
    }
}