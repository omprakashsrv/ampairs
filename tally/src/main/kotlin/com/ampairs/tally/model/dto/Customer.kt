package com.ampairs.tally.model.dto

import com.ampairs.network.customer.model.CustomerUpdateApiModel
import com.ampairs.tally.model.master.Ledger

fun List<Ledger>.toCustomers(): List<CustomerUpdateApiModel> {
    return map {
        CustomerUpdateApiModel(
            id = "",
            name = it.name ?: "",
            refId = it.guid ?: "",
            address = it.addressList?.joinToString(" ") ?: "",
            phone = parsePhone(it.ledgerMobile),
            landline = parsePhone(it.ledgerPhone),
            pincode = it.pinCode ?: "",
            gstin = it.partyGstin ?: "",
            state = it.ledStateName ?: "",
            email = "",
            countryCode = 91,
            latitude = null,
            longitude = null
        )
    }
}

private fun parsePhone(phone: String?): String {
    var phone = phone?.replace(" ", "") ?: phone
    phone = if (phone?.contains(",") == true) phone.split(",")[0] else
        (if (phone?.contains("/") == true) phone.split("/")[0] else
            (if (phone?.contains("\\") == true) phone.split("\\")[0] else phone))
            ?: ""
    phone = if (phone.startsWith("0")) phone.drop(1) else phone
    phone = "[^0-9]".toRegex().replace(phone, "")
    return phone
}