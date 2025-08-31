package com.ampairs.workspace.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.workspace.db.dao.WorkspaceDao
import com.ampairs.workspace.db.dao.WorkspaceMemberDao
import com.ampairs.workspace.db.dao.WorkspaceRoleDao
import com.ampairs.workspace.db.dao.WorkspacePermissionDao
import com.ampairs.workspace.db.entity.WorkspaceEntity
import com.ampairs.workspace.db.entity.WorkspaceMemberEntity
import com.ampairs.workspace.db.entity.WorkspaceRoleEntity
import com.ampairs.workspace.db.entity.WorkspacePermissionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        WorkspaceEntity::class, 
        WorkspaceMemberEntity::class,
        WorkspaceRoleEntity::class,
        WorkspacePermissionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class WorkspaceRoomDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun workspaceMemberDao(): WorkspaceMemberDao
    abstract fun workspaceRoleDao(): WorkspaceRoleDao
    abstract fun workspacePermissionDao(): WorkspacePermissionDao
}

