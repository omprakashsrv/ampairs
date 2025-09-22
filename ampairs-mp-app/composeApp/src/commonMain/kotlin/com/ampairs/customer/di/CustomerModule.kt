package com.ampairs.customer.di

import com.ampairs.customer.data.api.CustomerApi
import com.ampairs.customer.data.api.CustomerApiImpl
import com.ampairs.customer.data.db.CustomerDatabase
import com.ampairs.customer.data.repository.CustomerRepository
import com.ampairs.customer.domain.CustomerStore
import com.ampairs.customer.domain.StateStore
import com.ampairs.customer.ui.create.CustomerFormViewModel
import com.ampairs.customer.ui.details.CustomerDetailsViewModel
import com.ampairs.customer.ui.list.CustomersListViewModel
import com.ampairs.customer.ui.state.StateListViewModel
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
    single { get<CustomerDatabase>().stateDao() }

    // Repository Layer
    singleOf(::CustomerRepository)

    // Domain Layer
    singleOf(::CustomerStore)
    singleOf(::StateStore)

    // ViewModels
    factory { CustomersListViewModel(get(), get()) }
    factory { (customerId: String?) -> CustomerDetailsViewModel(customerId ?: "", get(), get()) }
    factory { (customerId: String?) -> CustomerFormViewModel(customerId, get(), get()) }
    factory { StateListViewModel(get()) }
}

expect val customerPlatformModule: org.koin.core.module.Module