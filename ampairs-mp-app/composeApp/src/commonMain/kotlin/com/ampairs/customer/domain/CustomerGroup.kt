package com.ampairs.customer.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerGroup(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    @SerialName("discount_percentage")
    val discountPercentage: Double? = null,
    val active: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

data class CustomerGroupListItem(
    val id: String,
    val name: String,
    val description: String?,
    val discountPercentage: Double?,
    val active: Boolean
)

fun CustomerGroup.toListItem(): CustomerGroupListItem = CustomerGroupListItem(
    id = id,
    name = name,
    description = description,
    discountPercentage = discountPercentage,
    active = active
)