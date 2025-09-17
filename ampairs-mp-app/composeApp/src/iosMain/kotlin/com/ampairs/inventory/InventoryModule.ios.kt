package com.ampairs.inventory

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.platform.getIosDatabasePath
import com.ampairs.inventory.db.InventoryRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val inventoryPlatformModule: Module = module {
    single<InventoryRoomDatabase> {
        Room.databaseBuilder<InventoryRoomDatabase>(
            name = getIosDatabasePath("inventory.db")
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(DispatcherProvider.io)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}