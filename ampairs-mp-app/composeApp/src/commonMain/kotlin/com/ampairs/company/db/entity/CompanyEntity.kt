package com.ampairs.company.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.company.domain.Company
import com.ampairs.domain.Location

@Entity(
    tableName = "companyEntity",
    indices = [Index(value = ["id"], unique = true)]
)
data class CompanyEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val name: String,
    val email: String? = null,
    val gstin: String? = null,
    val address: String? = null,
    val pincode: String? = null,
    val state: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val country_code: Long,
    val phone: String? = null,
    val last_updated: Long? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

fun CompanyEntity.asDomainModel(): Company {
    return Company(
        id = this.id,
        name = this.name,
        email = this.email,
        gstin = this.gstin,
        pincode = this.pincode,
        address = this.address,
        state = this.state,
        countryCode = this.country_code.toInt(),
        phone = this.phone,
        location = Location(latitude = latitude ?: 0.0, longitude = longitude ?: 0.0)
    )
}