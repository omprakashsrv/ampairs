package com.ampairs.customer.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customerEntity",
    indices = [Index(value = ["id"], unique = true)]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val company_id: String,
    val name: String,
    val email: String = "",
    val gstin: String = "",
    val address: String = "",
    val pincode: String = "",
    val state: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val country_code: Long = 91,
    val phone: String = "",
    val landline: String = "",
    val street: String = "",
    val street2: String = "",
    val city: String = "",
    val country: String = "",
    val billing_address: String? = null,
    val shipping_address: String? = null,
    val last_updated: Long = 0,
    val active: Long = 1,
    val billing_same_as_registered: Long = 1,
    val shipping_same_as_billing: Long = 1,
    val soft_deleted: Long = 0,
    val synced: Long = 0,
    val created_at: String = "",
    val updated_at: String = ""
)