package com.ampairs.customer.domain.model

import com.ampairs.tally.model.master.Ledger

fun Ledger.asDatabaseModel(): Customer {
    val customer = Customer()
    customer.name = this.name ?: ""
    customer.refId = this.guid ?: ""
    customer.address = this.addressList?.joinToString(" ") ?: ""
    customer.phone = parsePhone(this.ledgerMobile)
    customer.landline = parsePhone(this.ledgerPhone)
    customer.pincode = this.pinCode ?: ""
    customer.gstin = this.partyGstin ?: ""
    customer.state = this.ledStateName ?: ""
    customer.email = ""
    customer.countryCode = 91
    return customer
}

private fun parsePhone(phone: String?): String {
    var phone = phone?.replace(" ", "") ?: phone
    phone = if (phone?.contains(",") == true) phone.split(",")[0] else
        (if (phone?.contains("/") == true) phone.split("/")[0] else
            (if (phone?.contains("\\") == true) phone.split("\\")[0] else phone))
            ?: ""
    phone = if (phone.startsWith("0")) phone.drop(1) else phone
    phone = "[^0-9]".toRegex().replace(phone,"")
    return phone
}