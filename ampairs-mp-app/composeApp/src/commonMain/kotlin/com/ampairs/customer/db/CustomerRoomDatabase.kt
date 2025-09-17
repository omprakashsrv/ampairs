package com.ampairs.customer.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.customer.db.dao.CustomerDao
import com.ampairs.customer.db.entity.CustomerEntity

@Database(
    entities = [CustomerEntity::class],
    version = 1,
    exportSchema = true
)
@ConstructedBy(CustomerRoomDatabaseConstructor::class)
abstract class CustomerRoomDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object CustomerRoomDatabaseConstructor : RoomDatabaseConstructor<CustomerRoomDatabase>