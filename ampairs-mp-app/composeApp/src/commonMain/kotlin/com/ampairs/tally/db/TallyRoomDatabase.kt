package com.ampairs.tally.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ampairs.tally.db.dao.GodownDao
import com.ampairs.tally.db.dao.StockCategoryDao
import com.ampairs.tally.db.dao.StockGroupDao
import com.ampairs.tally.db.dao.StockItemDao
import com.ampairs.tally.db.dao.SyncAdapterDao
import com.ampairs.tally.db.dao.TallyInventoryDao
import com.ampairs.tally.db.dao.TallyUnitDao
import com.ampairs.tally.db.entity.GodownEntity
import com.ampairs.tally.db.entity.StockCategoryEntity
import com.ampairs.tally.db.entity.StockGroupEntity
import com.ampairs.tally.db.entity.StockItemEntity
import com.ampairs.tally.db.entity.SyncAdapterEntity
import com.ampairs.tally.db.entity.TallyInventoryEntity
import com.ampairs.tally.db.entity.TallyUnitEntity

@Database(
    entities = [
        SyncAdapterEntity::class,
        TallyUnitEntity::class,
        StockGroupEntity::class,
        StockCategoryEntity::class,
        StockItemEntity::class,
        GodownEntity::class,
        TallyInventoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class TallyRoomDatabase : RoomDatabase() {
    abstract fun syncAdapterDao(): SyncAdapterDao
    abstract fun tallyUnitDao(): TallyUnitDao
    abstract fun stockGroupDao(): StockGroupDao
    abstract fun stockCategoryDao(): StockCategoryDao
    abstract fun stockItemDao(): StockItemDao
    abstract fun godownDao(): GodownDao
    abstract fun tallyInventoryDao(): TallyInventoryDao
}