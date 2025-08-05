package com.ampairs.customer

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.customer.db.CustomerRoomDatabase
import getDatabaseDir
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

val customerPlatformModule: Module = module {
    single<CustomerRoomDatabase> {
        val dbFile = File(getDatabaseDir(), "customer.db")
        Room.databaseBuilder<CustomerRoomDatabase>(
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}