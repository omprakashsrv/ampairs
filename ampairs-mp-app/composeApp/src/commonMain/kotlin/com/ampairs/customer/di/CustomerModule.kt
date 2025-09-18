package com.ampairs.customer.di

import com.ampairs.customer.data.api.CustomerApi
import com.ampairs.customer.data.api.CustomerApiImpl
import com.ampairs.customer.data.db.CustomerDatabase
import com.ampairs.customer.data.repository.CustomerRepository
import com.ampairs.customer.domain.CustomerStore
import com.ampairs.customer.ui.create.CustomerFormViewModel
import com.ampairs.customer.ui.details.CustomerDetailsViewModel
import com.ampairs.customer.ui.list.CustomersListViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val customerModule = module {
    includes(customerPlatformModule)

    // API Layer
    singleOf(::CustomerApiImpl) bind CustomerApi::class

    // Database Layer
    single { get<CustomerDatabase>().customerDao() }

    // Repository Layer
    singleOf(::CustomerRepository)

    // Domain Layer
    singleOf(::CustomerStore)

    // ViewModels
    factory { CustomersListViewModel(get(), get()) }
    factory { (customerId: String?) -> CustomerDetailsViewModel(customerId ?: "", get(), get()) }
    factory { (customerId: String?) -> CustomerFormViewModel(customerId, get(), get()) }
}

expect val customerPlatformModule: org.koin.core.module.Module