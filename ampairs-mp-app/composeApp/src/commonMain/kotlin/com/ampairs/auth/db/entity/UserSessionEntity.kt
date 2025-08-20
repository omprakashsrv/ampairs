package com.ampairs.auth.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "userSessionEntity",
    indices = [Index(value = ["user_id"], unique = false)]
)
data class UserSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val user_id: String,  // Link to UserEntity.id
    val is_current: Boolean = false,  // True for the currently active user
    val company_id: String = "",  // Selected workspace for this user
    val last_login: Long = System.currentTimeMillis(),
    val login_count: Int = 1,
    val device_info: String = "",  // Device information
)