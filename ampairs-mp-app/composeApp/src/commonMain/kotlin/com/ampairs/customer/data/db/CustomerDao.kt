package com.ampairs.customer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers WHERE workspace_id = :workspaceId AND active = 1 ORDER BY name ASC")
    fun getAllCustomers(workspaceId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE workspace_id = :workspaceId AND id = :customerId")
    suspend fun getCustomerById(workspaceId: String, customerId: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE workspace_id = :workspaceId AND id = :customerId")
    fun observeCustomerById(workspaceId: String, customerId: String): Flow<CustomerEntity?>

    @Query("""
        SELECT * FROM customers
        WHERE workspace_id = :workspaceId
        AND active = 1
        AND (name LIKE '%' || :searchQuery || '%'
             OR phone LIKE '%' || :searchQuery || '%'
             OR email LIKE '%' || :searchQuery || '%')
        ORDER BY name ASC
    """)
    fun searchCustomers(workspaceId: String, searchQuery: String): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE workspace_id = :workspaceId AND id = :customerId")
    suspend fun deleteCustomer(workspaceId: String, customerId: String)

    @Query("SELECT COUNT(*) FROM customers WHERE workspace_id = :workspaceId AND active = 1")
    suspend fun getCustomerCount(workspaceId: String): Int

    @Query("SELECT * FROM customers WHERE workspace_id = :workspaceId AND synced = 0")
    suspend fun getUnsyncedCustomers(workspaceId: String): List<CustomerEntity>

    @Query("UPDATE customers SET synced = 1 WHERE workspace_id = :workspaceId AND id = :customerId")
    suspend fun markAsSynced(workspaceId: String, customerId: String)

    @Query("DELETE FROM customers WHERE workspace_id = :workspaceId")
    suspend fun clearWorkspaceCustomers(workspaceId: String)
}