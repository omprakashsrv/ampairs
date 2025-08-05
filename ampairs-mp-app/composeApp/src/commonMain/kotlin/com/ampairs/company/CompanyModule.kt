package com.ampairs.company

import com.ampairs.company.api.CompanyApi
import com.ampairs.company.api.CompanyApiImpl
import com.ampairs.company.db.CompanyRepository
import com.ampairs.company.db.CompanyRoomDatabase
import com.ampairs.company.viewmodel.CompanyListViewModel
import com.ampairs.company.viewmodel.CompanyViewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val companyModule: org.koin.core.module.Module = module {
    single { CompanyApiImpl(get(), get()) } bind (CompanyApi::class)
    // Database is provided by platform-specific modules
    single { get<CompanyRoomDatabase>().companyDao() }
    single { CompanyRepository(get(), get()) }
    
    // Direct ViewModel injection
    factory { CompanyListViewModel(get(), get()) }
    factory { (id: String?) -> CompanyViewModel(id, get()) }
}

fun companyModule() = companyModule