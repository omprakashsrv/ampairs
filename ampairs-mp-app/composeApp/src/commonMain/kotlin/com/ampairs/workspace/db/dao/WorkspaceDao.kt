package com.ampairs.workspace.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.workspace.db.entity.WorkspaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaces(workspaces: List<WorkspaceEntity>)

    @Query("SELECT * FROM workspaceEntity ORDER BY createdAt DESC")
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaceEntity WHERE id = :id")
    suspend fun getWorkspaceById(id: String): WorkspaceEntity?

    @Query("SELECT * FROM workspaceEntity WHERE slug = :slug")
    suspend fun getWorkspaceBySlug(slug: String): WorkspaceEntity?

    @Query("SELECT * FROM workspaceEntity WHERE name LIKE '%' || :searchQuery || '%' ORDER BY createdAt DESC")
    fun searchWorkspaces(searchQuery: String): Flow<List<WorkspaceEntity>>

    @Query("DELETE FROM workspaceEntity WHERE id = :id")
    suspend fun deleteWorkspace(id: String)

    @Query("DELETE FROM workspaceEntity")
    suspend fun deleteAllWorkspaces()

    @Query("SELECT COUNT(*) FROM workspaceEntity")
    suspend fun getWorkspaceCount(): Int
}