package com.ampairs.customer

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.platform.getIosDatabasePath
import com.ampairs.customer.db.CustomerRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val customerPlatformModule: Module = module {
    single<CustomerRoomDatabase> {
        Room.databaseBuilder<CustomerRoomDatabase>(
            name = getIosDatabasePath("customer.db")
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(DispatcherProvider.io)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}