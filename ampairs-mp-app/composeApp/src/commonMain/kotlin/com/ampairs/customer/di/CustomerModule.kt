package com.ampairs.customer.di

import com.ampairs.customer.data.api.CustomerApi
import com.ampairs.customer.data.api.CustomerApiImpl
import com.ampairs.customer.data.api.CustomerGroupApi
import com.ampairs.customer.data.api.CustomerGroupApiImpl
import com.ampairs.customer.data.api.CustomerTypeApi
import com.ampairs.customer.data.api.CustomerTypeApiImpl
import com.ampairs.customer.data.db.CustomerDatabase
import com.ampairs.customer.data.repository.CustomerRepository
import com.ampairs.customer.data.repository.CustomerGroupRepository
import com.ampairs.customer.data.repository.CustomerTypeRepository
import com.ampairs.customer.domain.CustomerStore
import com.ampairs.customer.domain.CustomerGroupStore
import com.ampairs.customer.domain.CustomerTypeStore
import com.ampairs.customer.domain.StateStore
import com.ampairs.customer.ui.create.CustomerFormViewModel
import com.ampairs.customer.ui.customergroup.CustomerGroupFormViewModel
import com.ampairs.customer.ui.customergroup.CustomerGroupListViewModel
import com.ampairs.customer.ui.customertype.CustomerTypeFormViewModel
import com.ampairs.customer.ui.customertype.CustomerTypeListViewModel
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
    singleOf(::CustomerTypeApiImpl) bind CustomerTypeApi::class
    singleOf(::CustomerGroupApiImpl) bind CustomerGroupApi::class

    // Database Layer
    single { get<CustomerDatabase>().customerDao() }
    single { get<CustomerDatabase>().customerTypeDao() }
    single { get<CustomerDatabase>().customerGroupDao() }
    single { get<CustomerDatabase>().stateDao() }

    // Repository Layer
    single { CustomerRepository(get(), get(), get()) }
    single { CustomerTypeRepository(get(), get(), get()) }
    single { CustomerGroupRepository(get(), get(), get()) }

    // Domain Layer
    singleOf(::CustomerStore)
    singleOf(::CustomerTypeStore)
    singleOf(::CustomerGroupStore)
    singleOf(::StateStore)

    // ViewModels
    factory { CustomersListViewModel(get()) }
    factory { (customerId: String?) -> CustomerDetailsViewModel(customerId ?: "", get(), get()) }
    factory { (customerId: String?) -> CustomerFormViewModel(customerId, get(), get(), get(), get()) }
    factory { StateListViewModel(get()) }

    // CustomerType ViewModels
    factory { CustomerTypeListViewModel(get()) }
    factory { (customerTypeId: String?) -> CustomerTypeFormViewModel(get(), customerTypeId) }

    // CustomerGroup ViewModels
    factory { CustomerGroupListViewModel(get()) }
    factory { (customerGroupId: String?) -> CustomerGroupFormViewModel(get(), customerGroupId) }
}

expect val customerPlatformModule: org.koin.core.module.Module