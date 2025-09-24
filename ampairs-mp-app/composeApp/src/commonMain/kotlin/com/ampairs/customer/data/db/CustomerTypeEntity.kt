package com.ampairs.customer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ampairs.customer.domain.CustomerType

@Entity(tableName = "customer_types")
data class CustomerTypeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val active: Boolean,
    val synced: Boolean = false,
    val createdAt: String?,
    val updatedAt: String?
)

fun CustomerTypeEntity.toCustomerType(): CustomerType = CustomerType(
    id = id,
    name = name,
    description = description,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CustomerType.toEntity(): CustomerTypeEntity = CustomerTypeEntity(
    id = id,
    name = name,
    description = description,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)