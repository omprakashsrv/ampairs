package com.ampairs.tally

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.platform.getIosDatabasePath
import com.ampairs.tally.db.TallyRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val tallyPlatformModule: Module = module {
    single<TallyRoomDatabase> {
        Room.databaseBuilder<TallyRoomDatabase>(
            name = getIosDatabasePath("tally.db")
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(DispatcherProvider.io)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}