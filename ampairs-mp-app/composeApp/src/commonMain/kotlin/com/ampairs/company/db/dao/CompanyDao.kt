package com.ampairs.company.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.company.db.entity.CompanyEntity

@Dao
interface CompanyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(company: CompanyEntity)

    @Query("SELECT * FROM companyEntity")
    suspend fun selectAll(): List<CompanyEntity>

    @Query("SELECT count(*) FROM companyEntity")
    suspend fun countCompanies(): Long

    @Query("SELECT * FROM companyEntity ORDER BY name ASC")
    fun companiesPaging(): PagingSource<Int, CompanyEntity>

    @Query("SELECT * FROM companyEntity WHERE id = :id")
    suspend fun selectById(id: String): CompanyEntity?
}