package com.ampairs.company

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.company.db.CompanyRoomDatabase
import getDatabaseDir
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

val companyPlatformModule: Module = module {
    single<CompanyRoomDatabase> {
        val dbFile = File(getDatabaseDir(), "company.db")
        Room.databaseBuilder<CompanyRoomDatabase>(
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}