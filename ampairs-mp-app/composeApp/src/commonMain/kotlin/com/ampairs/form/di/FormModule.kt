package com.ampairs.form.di

import com.ampairs.form.data.api.ConfigApi
import com.ampairs.form.data.api.ConfigApiImpl
import com.ampairs.form.data.repository.ConfigRepository
import org.koin.dsl.module

/**
 * Koin module for form configuration dependencies
 */
val formModule = module {
    // API
    single<ConfigApi> { ConfigApiImpl(get()) }

    // Repository
    single<ConfigRepository> { ConfigRepository(get()) }
}
