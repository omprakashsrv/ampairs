package com.ampairs.customer

import com.ampairs.customer.api.CustomerApi
import com.ampairs.customer.api.CustomerApiImpl
import com.ampairs.customer.db.CustomerRepository
import com.ampairs.customer.db.CustomerRoomDatabase
import com.ampairs.customer.viewmodel.CustomerViewModel
import com.ampairs.customer.viewmodel.CustomersViewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val customerModule: org.koin.core.module.Module = module {
    single { CustomerApiImpl(get(), get()) } bind (CustomerApi::class)
    // Database is provided by platform-specific modules
    single { get<CustomerRoomDatabase>().customerDao() }
    single { CustomerRepository(get(), get()) }
    
    // Direct ViewModel injection
    factory { CustomersViewModel(get(), get()) }
    factory { (id: String?) -> CustomerViewModel(id, get()) }
}

fun customerModule() = customerModule