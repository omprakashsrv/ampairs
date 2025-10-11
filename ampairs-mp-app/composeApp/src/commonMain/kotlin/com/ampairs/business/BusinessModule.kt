package com.ampairs.business

import com.ampairs.business.data.api.BusinessApi
import com.ampairs.business.data.api.BusinessApiImpl
import com.ampairs.business.data.db.BusinessDatabase
import com.ampairs.business.data.db.BusinessDao
import com.ampairs.business.data.repository.BusinessRepository
import com.ampairs.business.domain.BusinessStore
import com.ampairs.business.ui.BusinessProfileViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val businessModule: Module = module {
    single<BusinessDao> { get<BusinessDatabase>().businessDao() }

    single<BusinessApi> { BusinessApiImpl(get(), get()) }

    singleOf(::BusinessRepository)
    singleOf(::BusinessStore)

    viewModel { BusinessProfileViewModel(get(), get()) }
}

fun businessModule(): Module = businessModule
