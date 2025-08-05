package com.ampairs.auth.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.auth.db.entity.UserTokenEntity

@Dao
interface UserTokenDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserToken(userToken: UserTokenEntity)
    
    @Query("SELECT * FROM userTokenEntity")
    suspend fun selectAll(): List<UserTokenEntity>
    
    @Query("SELECT * FROM userTokenEntity WHERE id = :id")
    suspend fun selectById(id: String = "1"): UserTokenEntity?
}