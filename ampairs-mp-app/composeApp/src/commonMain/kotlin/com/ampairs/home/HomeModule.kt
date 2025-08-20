package com.ampairs.home

import org.koin.core.module.Module
import org.koin.dsl.module

val homeModule: Module = module {
    // Direct ViewModel injection
    factory { HomeScreenViewModel() }
}

fun homeModule() = homeModule