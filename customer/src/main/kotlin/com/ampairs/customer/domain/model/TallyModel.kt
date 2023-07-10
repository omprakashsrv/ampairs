package com.ampairs.customer.domain.model

import com.ampairs.core.user.model.Customer
import com.ampairs.tally.model.master.Ledger

fun Ledger.asDatabaseModel(): Customer {
    val customer = Customer()
    return customer
}