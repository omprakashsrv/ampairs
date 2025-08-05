package com.ampairs.company.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ampairs.company.domain.Company

class CompanyState(val company: Company) {
    var name by mutableStateOf(this.company.name)
    var gstin by mutableStateOf(this.company.gstin)
    var email by mutableStateOf(this.company.email)
    var address by mutableStateOf(this.company.address)
    var pincode by mutableStateOf(this.company.pincode)
    var state by mutableStateOf(this.company.state)
    var countryCode by mutableStateOf(this.company.countryCode)
    var phone by mutableStateOf(this.company.phone)
    var landline by mutableStateOf(this.company.landline)
    var location by mutableStateOf(this.company.location)
}

fun CompanyState.toDomainModel(): Company {
    this.company.name = this.name
    this.company.email = this.email
    this.company.gstin = this.gstin
    this.company.address = this.address
    this.company.pincode = this.pincode
    this.company.state = this.state
    this.company.countryCode = this.countryCode
    this.company.phone = this.phone
    this.company.landline = this.landline
    this.company.location = this.location
    return this.company
}