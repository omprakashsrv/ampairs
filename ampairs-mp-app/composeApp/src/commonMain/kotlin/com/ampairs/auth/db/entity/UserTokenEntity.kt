package com.ampairs.auth.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "userTokenEntity",
    indices = [Index(value = ["id"], unique = true)]
)
data class UserTokenEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val refresh_token: String,
    val access_token: String,
    val access_token_expires_at: Long? = null,
    val refresh_token_expires_at: Long? = null,
    @Deprecated("Use access_token_expires_at instead")
    val expires_at: Long? = null,
)