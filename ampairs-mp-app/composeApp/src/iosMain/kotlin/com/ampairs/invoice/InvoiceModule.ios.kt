package com.ampairs.invoice

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.platform.getIosDatabasePath
import com.ampairs.invoice.db.InvoiceRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val invoicePlatformModule: Module = module {
    single<InvoiceRoomDatabase> {
        Room.databaseBuilder<InvoiceRoomDatabase>(
            name = getIosDatabasePath("invoice.db")
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(DispatcherProvider.io)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}