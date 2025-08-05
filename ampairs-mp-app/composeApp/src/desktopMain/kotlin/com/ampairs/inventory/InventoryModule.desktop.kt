package com.ampairs.inventory

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.inventory.db.InventoryRoomDatabase
import getDatabaseDir
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

val inventoryPlatformModule: Module = module {
    single<InventoryRoomDatabase> {
        val dbFile = File(getDatabaseDir(), "inventory.db")
        Room.databaseBuilder<InventoryRoomDatabase>(
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}