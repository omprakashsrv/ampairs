package com.ampairs.product

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.platform.getIosDatabasePath
import com.ampairs.product.db.ProductRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val productPlatformModule: Module = module {
    single<ProductRoomDatabase> {
        Room.databaseBuilder<ProductRoomDatabase>(
            name = getIosDatabasePath("product.db")
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(DispatcherProvider.io)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}