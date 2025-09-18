package com.ampairs.customer.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerAddress
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["workspace_id"]),
        Index(value = ["name"])
    ]
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String?,
    val phone: String?,
    val country_code: Int,
    val gstin: String?,
    val address: String?,
    val street: String?,
    val city: String?,
    val state: String?,
    val pincode: String?,
    val country: String,
    val billing_address_json: String?,
    val shipping_address_json: String?,
    val active: Boolean,
    val workspace_id: String,
    val created_at: String?,
    val updated_at: String?,
    val synced: Boolean = false,
    val last_sync: Long = 0
)

@OptIn(ExperimentalTime::class)
fun Customer.toEntity(): CustomerEntity = CustomerEntity(
    id = id,
    name = name,
    email = email,
    phone = phone,
    country_code = countryCode,
    gstin = gstin,
    address = address,
    street = street,
    city = city,
    state = state,
    pincode = pincode,
    country = country,
    billing_address_json = billingAddress?.let { Json.encodeToString(it) },
    shipping_address_json = shippingAddress?.let { Json.encodeToString(it) },
    active = active,
    workspace_id = workspaceId,
    created_at = createdAt,
    updated_at = updatedAt,
    synced = false,
    last_sync = Clock.System.now().toEpochMilliseconds()
)

fun CustomerEntity.toDomain(): Customer = Customer(
    id = id,
    name = name,
    email = email,
    phone = phone,
    countryCode = country_code,
    gstin = gstin,
    address = address,
    street = street,
    city = city,
    state = state,
    pincode = pincode,
    country = country,
    billingAddress = billing_address_json?.let {
        try { Json.decodeFromString<CustomerAddress>(it) } catch (e: Exception) { null }
    },
    shippingAddress = shipping_address_json?.let {
        try { Json.decodeFromString<CustomerAddress>(it) } catch (e: Exception) { null }
    },
    active = active,
    workspaceId = workspace_id,
    createdAt = created_at,
    updatedAt = updated_at
)