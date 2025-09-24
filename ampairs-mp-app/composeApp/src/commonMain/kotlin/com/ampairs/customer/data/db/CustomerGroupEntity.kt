package com.ampairs.customer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ampairs.customer.domain.CustomerGroup

@Entity(tableName = "customer_groups")
data class CustomerGroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val discountPercentage: Double?,
    val active: Boolean,
    val synced: Boolean = false,
    val createdAt: String?,
    val updatedAt: String?
)

fun CustomerGroupEntity.toCustomerGroup(): CustomerGroup = CustomerGroup(
    id = id,
    name = name,
    description = description,
    discountPercentage = discountPercentage,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CustomerGroup.toEntity(): CustomerGroupEntity = CustomerGroupEntity(
    id = id,
    name = name,
    description = description,
    discountPercentage = discountPercentage,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)