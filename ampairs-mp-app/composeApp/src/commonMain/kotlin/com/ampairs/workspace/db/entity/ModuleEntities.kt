package com.ampairs.workspace.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Simple database entities that match the web service models
 * Following the same structure as other workspace entities
 */

@Entity(
    tableName = "installed_module",
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["workspaceId"], unique = false),
        Index(value = ["workspaceId", "moduleCode"], unique = true), // One module per workspace
        Index(value = ["moduleCode"], unique = false),
        Index(value = ["status"], unique = false),
        Index(value = ["category"], unique = false),
        Index(value = ["sync_state"], unique = false),
        Index(value = ["last_synced_at"], unique = false)
    ]
)
data class InstalledModuleEntity(
    @PrimaryKey
    val id: String,
    val workspaceId: String, // Multi-tenant support - modules are installed per workspace
    val moduleCode: String,
    val name: String,
    val category: String,
    val version: String,
    val status: String, // ACTIVE | INSTALLED | INACTIVE
    val enabled: Boolean,
    val installedAt: String,
    val icon: String,
    val primaryColor: String,
    val healthScore: Double? = null,
    val needsAttention: Boolean? = null,
    val description: String? = null,
    
    // Store5 sync metadata - following existing pattern
    val sync_state: String = "SYNCED", // SYNCED | PENDING | UPLOADING | ERROR
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
    val last_synced_at: Long? = null,
)

@Entity(
    tableName = "available_module",
    indices = [
        Index(value = ["moduleCode"], unique = true),
        Index(value = ["category"], unique = false),
        Index(value = ["featured"], unique = false),
        Index(value = ["sync_state"], unique = false),
        Index(value = ["last_synced_at"], unique = false)
    ]
)
data class AvailableModuleEntity(
    @PrimaryKey
    val moduleCode: String,
    val name: String,
    val description: String? = null,
    val category: String,
    val version: String,
    val rating: Double,
    val installCount: Int,
    val complexity: String,
    val icon: String,
    val primaryColor: String,
    val featured: Boolean,
    val requiredTier: String,
    val sizeMb: Int,
    
    // Store5 sync metadata - following existing pattern
    val sync_state: String = "SYNCED",
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
    val last_synced_at: Long? = null,
)