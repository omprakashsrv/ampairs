package com.ampairs.customer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO for customer image database operations with workspace-aware queries.
 * Supports offline-first patterns with sync metadata tracking.
 */
@Dao
interface CustomerImageDao {

    // Basic CRUD operations

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerImage(customerImage: CustomerImageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerImages(customerImages: List<CustomerImageEntity>)

    @Update
    suspend fun updateCustomerImage(customerImage: CustomerImageEntity)

    @Query("DELETE FROM customer_images WHERE uid = :uid AND workspace_id = :workspaceId")
    suspend fun deleteCustomerImage(uid: String, workspaceId: String)

    @Query("DELETE FROM customer_images WHERE customer_id = :customerId AND workspace_id = :workspaceId")
    suspend fun deleteCustomerImagesByCustomer(customerId: String, workspaceId: String)

    // Workspace-aware queries

    @Query("SELECT * FROM customer_images WHERE uid = :uid AND workspace_id = :workspaceId")
    suspend fun getCustomerImage(uid: String, workspaceId: String): CustomerImageEntity?

    @Query("SELECT * FROM customer_images WHERE customer_id = :customerId AND workspace_id = :workspaceId ORDER BY sort_order ASC, created_at DESC")
    fun observeCustomerImages(customerId: String, workspaceId: String): Flow<List<CustomerImageEntity>>

    @Query("SELECT * FROM customer_images WHERE customer_id = :customerId AND workspace_id = :workspaceId ORDER BY sort_order ASC, created_at DESC")
    suspend fun getCustomerImages(customerId: String, workspaceId: String): List<CustomerImageEntity>

    @Query("SELECT * FROM customer_images WHERE customer_id = :customerId AND is_primary = 1 AND workspace_id = :workspaceId LIMIT 1")
    suspend fun getPrimaryCustomerImage(customerId: String, workspaceId: String): CustomerImageEntity?

    @Query("SELECT * FROM customer_images WHERE customer_id = :customerId AND is_primary = 1 AND workspace_id = :workspaceId LIMIT 1")
    fun observePrimaryCustomerImage(customerId: String, workspaceId: String): Flow<CustomerImageEntity?>

    // Sync-related queries

    @Query("SELECT * FROM customer_images WHERE synced = 0 AND workspace_id = :workspaceId ORDER BY local_created_at ASC")
    suspend fun getUnsyncedCustomerImages(workspaceId: String): List<CustomerImageEntity>

    @Query("SELECT * FROM customer_images WHERE sync_pending = 1 AND workspace_id = :workspaceId ORDER BY local_updated_at ASC")
    suspend fun getPendingSyncCustomerImages(workspaceId: String): List<CustomerImageEntity>

    @Query("SELECT * FROM customer_images WHERE upload_status = :status AND workspace_id = :workspaceId")
    suspend fun getCustomerImagesByUploadStatus(status: String, workspaceId: String): List<CustomerImageEntity>

    @Query("UPDATE customer_images SET synced = :synced, sync_pending = :syncPending, last_sync_attempt = :lastSyncAttempt WHERE uid = :uid AND workspace_id = :workspaceId")
    suspend fun updateSyncStatus(uid: String, workspaceId: String, synced: Boolean, syncPending: Boolean, lastSyncAttempt: String?)

    @Query("UPDATE customer_images SET upload_status = :status, local_updated_at = :updatedAt WHERE uid = :uid AND workspace_id = :workspaceId")
    suspend fun updateUploadStatus(uid: String, workspaceId: String, status: String, updatedAt: String)

    // Primary image management

    @Query("UPDATE customer_images SET is_primary = 0, local_updated_at = :updatedAt WHERE customer_id = :customerId AND workspace_id = :workspaceId")
    suspend fun clearPrimaryImages(customerId: String, workspaceId: String, updatedAt: String)

    @Query("UPDATE customer_images SET is_primary = 1, local_updated_at = :updatedAt WHERE uid = :uid AND workspace_id = :workspaceId")
    suspend fun setPrimaryImage(uid: String, workspaceId: String, updatedAt: String)

    // Batch operations

    @Query("DELETE FROM customer_images WHERE uid IN (:uids) AND workspace_id = :workspaceId")
    suspend fun deleteCustomerImagesByIds(uids: List<String>, workspaceId: String)

    @Query("UPDATE customer_images SET sort_order = :sortOrder, local_updated_at = :updatedAt WHERE uid = :uid AND workspace_id = :workspaceId")
    suspend fun updateSortOrder(uid: String, workspaceId: String, sortOrder: Int, updatedAt: String)

    // File management

    @Query("SELECT local_path FROM customer_images WHERE local_path IS NOT NULL AND workspace_id = :workspaceId")
    suspend fun getAllLocalPaths(workspaceId: String): List<String>

    @Query("UPDATE customer_images SET local_path = :localPath, local_updated_at = :updatedAt WHERE uid = :uid AND workspace_id = :workspaceId")
    suspend fun updateLocalPath(uid: String, workspaceId: String, localPath: String?, updatedAt: String)

    // Statistics and counts

    @Query("SELECT COUNT(*) FROM customer_images WHERE customer_id = :customerId AND workspace_id = :workspaceId")
    suspend fun getCustomerImageCount(customerId: String, workspaceId: String): Int

    @Query("SELECT COUNT(*) FROM customer_images WHERE synced = 0 AND workspace_id = :workspaceId")
    suspend fun getUnsyncedCount(workspaceId: String): Int

    @Query("SELECT COUNT(*) FROM customer_images WHERE upload_status = 'PENDING' AND workspace_id = :workspaceId")
    suspend fun getPendingUploadCount(workspaceId: String): Int

    // Search and filtering

    @Query("""
        SELECT * FROM customer_images
        WHERE customer_id = :customerId
        AND workspace_id = :workspaceId
        AND (file_name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY sort_order ASC, created_at DESC
    """)
    suspend fun searchCustomerImages(customerId: String, workspaceId: String, query: String): List<CustomerImageEntity>

    @Query("SELECT * FROM customer_images WHERE workspace_id = :workspaceId AND updated_at > :lastSyncTime ORDER BY updated_at ASC")
    suspend fun getModifiedSince(workspaceId: String, lastSyncTime: String): List<CustomerImageEntity>

    // Cleanup operations

    @Query("DELETE FROM customer_images WHERE workspace_id = :workspaceId")
    suspend fun deleteAllForWorkspace(workspaceId: String)

    @Query("DELETE FROM customer_images WHERE local_path IS NOT NULL AND synced = 1 AND workspace_id = :workspaceId")
    suspend fun deleteLocalFilesForSyncedImages(workspaceId: String)
}