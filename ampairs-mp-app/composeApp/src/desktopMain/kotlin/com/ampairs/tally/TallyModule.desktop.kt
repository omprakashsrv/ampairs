package com.ampairs.tally

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.tally.db.TallyRoomDatabase
import getDatabaseDir
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

val tallyPlatformModule: Module = module {
    single<TallyRoomDatabase> {
        val dbFile = File(getDatabaseDir(), "tally.db")
        Room.databaseBuilder<TallyRoomDatabase>(
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}