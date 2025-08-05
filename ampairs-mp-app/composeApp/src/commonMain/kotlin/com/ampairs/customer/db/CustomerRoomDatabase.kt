package com.ampairs.customer.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ampairs.customer.db.dao.CustomerDao
import com.ampairs.customer.db.entity.CustomerEntity

@Database(
    entities = [CustomerEntity::class],
    version = 1,
    exportSchema = true
)
abstract class CustomerRoomDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
}