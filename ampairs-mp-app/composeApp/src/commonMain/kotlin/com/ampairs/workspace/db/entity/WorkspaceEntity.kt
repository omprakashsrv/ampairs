package com.ampairs.workspace.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workspaceEntity",
    indices = [
        Index(
            value = ["id"],
            unique = false
        ), // Remove unique constraint to allow multiple users to have same workspace
        Index(value = ["user_id"], unique = false),
        Index(value = ["user_id", "id"], unique = true) // Unique per user
    ]
)
data class WorkspaceEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val user_id: String = "", // Associate workspace with user
    val name: String,
    val slug: String,
    val description: String = "",
    val workspaceType: String = "BUSINESS",
    val avatarUrl: String = "",
    val isActive: Boolean = true,
    val subscriptionPlan: String = "FREE",
    val maxMembers: Int = 5,
    val storageLimitGb: Int = 1,
    val storageUsedGb: Int = 0,
    val timezone: String = "UTC",
    val language: String = "en",
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String,
    val lastActivityAt: String = "",
    val trialExpiresAt: String = "",
    val memberCount: Int = 1,
    val isTrial: Boolean = false,
    val storagePercentage: Double = 0.0,
)