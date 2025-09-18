package com.ampairs.customer.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [CustomerEntity::class],
    version = 1,
    exportSchema = true
)
@ConstructedBy(CustomerDatabaseConstructor::class)
abstract class CustomerDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object CustomerDatabaseConstructor : RoomDatabaseConstructor<CustomerDatabase>