package com.ampairs.customer

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.customer.db.CustomerRoomDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val customerPlatformModule: Module = module {
    single<CustomerRoomDatabase> {
        val context = androidContext()
        val dbFile = context.getDatabasePath("customer.db")
        Room.databaseBuilder<CustomerRoomDatabase>(
            context = context,
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}