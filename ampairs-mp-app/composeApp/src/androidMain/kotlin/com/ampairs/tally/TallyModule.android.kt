package com.ampairs.tally

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.tally.db.TallyRoomDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val tallyPlatformModule: Module = module {
    single<TallyRoomDatabase> {
        val context = androidContext()
        val dbFile = context.getDatabasePath("tally.db")
        Room.databaseBuilder<TallyRoomDatabase>(
            context = context,
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}