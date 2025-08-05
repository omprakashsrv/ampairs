package com.ampairs.inventory.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ampairs.inventory.db.dao.InventoryDao
import com.ampairs.inventory.db.entity.InventoryEntity

@Database(
    entities = [InventoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class InventoryRoomDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
}