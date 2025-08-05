package com.ampairs.auth.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ampairs.auth.db.dao.UserDao
import com.ampairs.auth.db.dao.UserTokenDao
import com.ampairs.auth.db.entity.UserEntity
import com.ampairs.auth.db.entity.UserTokenEntity

@Database(
    entities = [UserEntity::class, UserTokenEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AuthRoomDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userTokenDao(): UserTokenDao
}

