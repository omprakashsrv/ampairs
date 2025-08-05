package com.ampairs.invoice

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.invoice.db.InvoiceRoomDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val invoicePlatformModule: Module = module {
    single<InvoiceRoomDatabase> {
        val context = androidContext()
        val dbFile = context.getDatabasePath("invoice.db")
        Room.databaseBuilder<InvoiceRoomDatabase>(
            context = context,
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}