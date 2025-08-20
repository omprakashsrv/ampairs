package com.ampairs.auth.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ampairs.auth.db.entity.UserSessionEntity

@Dao
interface UserSessionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userSession: UserSessionEntity)
    
    @Update
    suspend fun update(userSession: UserSessionEntity)
    
    @Query("SELECT * FROM userSessionEntity")
    suspend fun selectAll(): List<UserSessionEntity>
    
    @Query("SELECT * FROM userSessionEntity WHERE user_id = :userId")
    suspend fun selectByUserId(userId: String): UserSessionEntity?
    
    @Query("SELECT * FROM userSessionEntity WHERE is_current = 1 LIMIT 1")
    suspend fun getCurrentUserSession(): UserSessionEntity?
    
    @Query("UPDATE userSessionEntity SET is_current = 0")
    suspend fun clearCurrentUser()
    
    @Query("UPDATE userSessionEntity SET is_current = 1 WHERE user_id = :userId")
    suspend fun setCurrentUser(userId: String)
    
    @Query("UPDATE userSessionEntity SET company_id = :companyId WHERE user_id = :userId")
    suspend fun updateCompanyId(userId: String, companyId: String)
    
    @Query("UPDATE userSessionEntity SET last_login = :lastLogin, login_count = login_count + 1 WHERE user_id = :userId")
    suspend fun updateLoginInfo(userId: String, lastLogin: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM userSessionEntity WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)
    
    @Query("SELECT * FROM userSessionEntity ORDER BY last_login DESC")
    suspend fun selectAllOrderedByLastLogin(): List<UserSessionEntity>
}