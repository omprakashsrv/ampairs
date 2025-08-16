package com.ampairs.workspace.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.workspace.db.dao.WorkspaceDao
import com.ampairs.workspace.db.entity.WorkspaceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [WorkspaceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WorkspaceRoomDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
}

