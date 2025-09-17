package com.ampairs.inventory

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.inventory.db.InventoryRoomDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val inventoryPlatformModule: Module = module {
    single<InventoryRoomDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createAndroidDatabase(
            klass = InventoryRoomDatabase::class,
            context = androidContext(),
            queryDispatcher = Dispatchers.IO,
            moduleName = "inventory"
        )
    }
}