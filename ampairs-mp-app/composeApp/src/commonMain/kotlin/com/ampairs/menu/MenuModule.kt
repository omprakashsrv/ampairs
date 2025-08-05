package com.ampairs.menu

import org.koin.dsl.module

val menuModule: org.koin.core.module.Module = module {
    single { AppNavigator() }
}

fun menuModule() = menuModule