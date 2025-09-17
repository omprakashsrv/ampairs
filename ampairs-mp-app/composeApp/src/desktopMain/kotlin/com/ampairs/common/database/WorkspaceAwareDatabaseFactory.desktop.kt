package com.ampairs.common.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual inline fun <reified T : RoomDatabase> WorkspaceAwareDatabaseFactory.createPlatformDatabase(dbPath: String): T {
    return Room.databaseBuilder<T>(
        name = dbPath
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryDispatcher)
        .fallbackToDestructiveMigration(true)
        .build()
}