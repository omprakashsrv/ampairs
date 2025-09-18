package com.ampairs.customer.compat

import com.ampairs.customer.domain.Customer

// Temporary compatibility bridge for modules that depend on old customer structure
// TODO: Update dependent modules to use new customer module architecture

@Deprecated("Use new CustomerStore instead", ReplaceWith("CustomerStore"))
class CustomerRepository {
    suspend fun getCustomer(customerId: String): Customer? = null
    suspend fun updateCustomers(customers: List<Customer>): Any = Unit
}

@Deprecated("Use new CustomerDao instead")
class CustomerDao {
    suspend fun selectById(id: String): Customer? = null
}

// Legacy constants for dependent modules
object Constants {
    const val DEFAULT_STRING = ""
    const val PAGE_SIZE = 20
}