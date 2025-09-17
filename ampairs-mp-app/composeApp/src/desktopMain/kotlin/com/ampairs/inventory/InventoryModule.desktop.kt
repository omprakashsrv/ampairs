package com.ampairs.inventory

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.inventory.db.InventoryRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val inventoryPlatformModule: Module = module {
    single<InventoryRoomDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createDatabase(
            klass = InventoryRoomDatabase::class,
            moduleName = "inventory"
        )
    }
}