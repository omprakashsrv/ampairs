package com.ampairs.core.domain.dto

import com.ampairs.core.domain.model.Company

class CompanyResponse(
    var id: String,
    var countryCode: Int,
    var name: String,
    var phone: String,
    var email: String?,
    var gstin: String?,
    var address: String?,
    var pincode: String?,
    var state: String?,
    var latitude: Double?,
    var longitude: Double?
)


fun CompanyResponse.toCompanyResponse(company: Company): CompanyResponse {
    return CompanyResponse(
        id = company.id,
        name = company.name,
        countryCode = company.countryCode,
        phone = company.phone,
        email = company.email,
        gstin = company.gstin,
        address = company.address,
        pincode = company.pincode,
        state = company.state,
        latitude = company.location?.x,
        longitude = company.location?.y
    )
}

fun List<Company>.toCompanyResponse(): List<CompanyResponse> {
    return map {
        CompanyResponse(
            id = it.id,
            name = it.name,
            countryCode = it.countryCode,
            phone = it.phone,
            email = it.email,
            gstin = it.gstin,
            address = it.address,
            pincode = it.pincode,
            state = it.state,
            latitude = it.location?.x,
            longitude = it.location?.y
        )
    }
}