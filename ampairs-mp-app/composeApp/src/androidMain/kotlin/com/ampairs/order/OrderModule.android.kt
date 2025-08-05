package com.ampairs.order

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.order.db.OrderRoomDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val orderPlatformModule: Module = module {
    single<OrderRoomDatabase> {
        val context = androidContext()
        val dbFile = context.getDatabasePath("order.db")
        Room.databaseBuilder<OrderRoomDatabase>(
            context = context,
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}