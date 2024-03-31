package com.ampairs.company.model.dto

import com.ampairs.company.model.Company


class CompanyResponse(
    var id: String,
    var countryCode: Int,
    var name: String,
    var phone: String,
    var landline: String,
    var email: String?,
    var gstin: String?,
    var address: String?,
    var pincode: String?,
    var state: String?,
    var latitude: Double?,
    var longitude: Double?,
    var lastUpdated: Long,
    var createdAt: String?,
    var updatedAt: String?,
)


fun CompanyResponse.toCompanyResponse(company: Company): CompanyResponse {
    return CompanyResponse(
        id = company.id,
        name = company.name,
        countryCode = company.countryCode,
        phone = company.phone,
        landline = company.landline,
        email = company.email,
        gstin = company.gstin,
        address = company.address,
        pincode = company.pincode,
        state = company.state,
        latitude = company.location?.x,
        longitude = company.location?.y,
        createdAt = company.createdAt,
        updatedAt = company.updatedAt,
        lastUpdated = company.lastUpdated
    )
}

fun List<Company>.toCompanyResponse(): List<CompanyResponse> {
    return map {
        it.toCompanyResponse()
    }
}

fun Company.toCompanyResponse() = CompanyResponse(
    id = this.id,
    name = this.name,
    countryCode = this.countryCode,
    phone = this.phone,
    email = this.email,
    gstin = this.gstin,
    address = this.address,
    pincode = this.pincode,
    state = this.state,
    latitude = this.location?.x,
    longitude = this.location?.y,
    lastUpdated = this.lastUpdated,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    landline = this.landline
)