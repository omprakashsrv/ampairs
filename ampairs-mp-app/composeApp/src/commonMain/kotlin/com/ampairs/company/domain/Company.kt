package com.ampairs.company.domain

import com.ampairs.company.api.model.CompanyApiModel
import com.ampairs.company.db.entity.CompanyEntity
import com.ampairs.domain.Location

data class Company(
    var id: String = "",
    var name: String = "",
    var email: String? = null,
    var gstin: String? = null,
    var address: String? = null,
    var pincode: String? = null,
    var state: String? = null,
    var location: Location? = null,
    var countryCode: Int = 91,
    var phone: String? = null,
    var landline: String? = null,
)

fun List<CompanyApiModel>.asDatabaseModel(): List<CompanyEntity> {
    return map {
        it.asDatabaseModel()
    }
}

fun CompanyApiModel.asDatabaseModel() = CompanyEntity(
    seq_id = 0,
    id = this.id,
    name = this.name,
    email = this.email,
    gstin = this.gstin,
    pincode = this.pincode,
    address = this.address,
    state = this.state,
    latitude = this.latitude,
    longitude = this.longitude,
    country_code = this.countryCode.toLong(),
    phone = this.phone,
    created_at = this.createdAt,
    updated_at = this.updatedAt,
    last_updated = this.lastUpdated
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


fun Company.asApiModel(): CompanyApiModel {
    return CompanyApiModel(
        id = this.id,
        name = this.name,
        email = this.email,
        gstin = this.gstin,
        pincode = this.pincode,
        address = this.address,
        state = this.state,
        countryCode = this.countryCode,
        phone = this.phone,
        latitude = this.location?.latitude,
        longitude = this.location?.longitude,
        landline = this.landline,
        createdAt = "",
        updatedAt = "",
        lastUpdated = 0
    )
}