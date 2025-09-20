package com.ampairs.tax

import com.ampairs.tax.data.api.TaxApi
import com.ampairs.tax.data.api.TaxApiImpl
import com.ampairs.tax.data.repository.TaxRepository
import com.ampairs.tax.domain.TaxCalculationEngine
import com.ampairs.tax.domain.TaxStore
import com.ampairs.tax.ui.calculator.TaxCalculatorViewModel
import com.ampairs.tax.ui.hsn.HsnCodesListViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val taxModule = module {

    // API Layer
    singleOf(::TaxApiImpl) bind TaxApi::class

    // Database Layer
    single { get<com.ampairs.tax.data.db.TaxRoomDatabase>().hsnCodeDao() }
    single { get<com.ampairs.tax.data.db.TaxRoomDatabase>().taxRateDao() }

    // Repository Layer
    singleOf(::TaxRepository)

    // Domain Layer
    singleOf(::TaxStore)
    singleOf(::TaxCalculationEngine)

    // ViewModels
    factory { HsnCodesListViewModel(get()) }
    factory { TaxCalculatorViewModel(get()) }
}