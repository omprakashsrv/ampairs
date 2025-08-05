package com.ampairs.company.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ampairs.company.db.dao.CompanyDao
import com.ampairs.company.db.entity.CompanyEntity

@Database(
    entities = [CompanyEntity::class],
    version = 1,
    exportSchema = true
)
abstract class CompanyRoomDatabase : RoomDatabase() {
    abstract fun companyDao(): CompanyDao
}