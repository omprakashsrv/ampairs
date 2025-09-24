package com.ampairs.customer.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerType(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val active: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

data class CustomerTypeListItem(
    val id: String,
    val name: String,
    val description: String?,
    val active: Boolean
)

fun CustomerType.toListItem(): CustomerTypeListItem = CustomerTypeListItem(
    id = id,
    name = name,
    description = description,
    active = active
)