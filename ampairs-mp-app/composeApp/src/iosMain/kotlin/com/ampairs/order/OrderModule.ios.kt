package com.ampairs.order

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.platform.getIosDatabasePath
import com.ampairs.order.db.OrderRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val orderPlatformModule: Module = module {
    single<OrderRoomDatabase> {
        Room.databaseBuilder<OrderRoomDatabase>(
            name = getIosDatabasePath("order.db")
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(DispatcherProvider.io)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}