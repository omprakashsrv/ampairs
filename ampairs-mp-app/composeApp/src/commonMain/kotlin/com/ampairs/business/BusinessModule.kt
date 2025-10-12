package com.ampairs.business

import com.ampairs.business.data.api.BusinessApi
import com.ampairs.business.data.api.BusinessApiImpl
import com.ampairs.business.data.db.BusinessDatabase
import com.ampairs.business.data.db.BusinessDao
import com.ampairs.business.data.repository.BusinessRepository
import com.ampairs.business.domain.BusinessStore
import com.ampairs.business.ui.BusinessProfileViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val businessModule: Module = module {
    // Use factory scope for workspace-aware components (Constitution Principle III)
    factory<BusinessDao> { get<BusinessDatabase>().businessDao() }

    factory<BusinessApi> { BusinessApiImpl(get(), get()) }

    factory { BusinessRepository(get(), get(), get()) }
    factory { BusinessStore(get()) }

    viewModel { BusinessProfileViewModel(get(), get()) }
}

fun businessModule(): Module = businessModule
