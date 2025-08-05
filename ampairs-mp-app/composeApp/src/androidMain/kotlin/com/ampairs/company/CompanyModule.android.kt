package com.ampairs.company

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.company.db.CompanyRoomDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val companyPlatformModule: Module = module {
    single<CompanyRoomDatabase> {
        val context = androidContext()
        val dbFile = context.getDatabasePath("company.db")
        Room.databaseBuilder<CompanyRoomDatabase>(
            context = context,
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}