package com.ampairs.customer.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.customer.db.entity.CustomerEntity

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE) 
    suspend fun update(customer: CustomerEntity)
    
    @Query("SELECT * FROM customerEntity")
    suspend fun selectAll(): List<CustomerEntity>
    
    @Query("SELECT * FROM customerEntity WHERE synced=0")
    suspend fun unSyncedCustomers(): List<CustomerEntity>
    
    @Query("SELECT * FROM customerEntity WHERE id = :id")
    suspend fun selectById(id: String): CustomerEntity?
    
    @Query("SELECT max(last_updated) FROM customerEntity")
    suspend fun getLastUpdated(): Long?
    
    @Query("SELECT count(*) FROM customerEntity")
    suspend fun countCustomers(): Long
    
    @Query("SELECT * FROM customerEntity ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun customers(limit: Int, offset: Int): List<CustomerEntity>
    
    @Query("SELECT count(*) FROM customerEntity WHERE name LIKE ('%' || :name || '%')")
    suspend fun countCustomersByName(name: String): Long
    
    @Query("SELECT * FROM customerEntity WHERE name LIKE ('%' || :name || '%') ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun customersByName(name: String, limit: Int, offset: Int): List<CustomerEntity>
    
    @Query("SELECT * FROM customerEntity WHERE company_id = :id")
    suspend fun selectByCompanyId(id: String): List<CustomerEntity>
}