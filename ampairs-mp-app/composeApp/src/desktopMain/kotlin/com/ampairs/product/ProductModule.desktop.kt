package com.ampairs.product

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.product.db.ProductRoomDatabase
import getDatabaseDir
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

val productPlatformModule: Module = module {
    single<ProductRoomDatabase> {
        val dbFile = File(getDatabaseDir(), "product.db")
        Room.databaseBuilder<ProductRoomDatabase>(
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}