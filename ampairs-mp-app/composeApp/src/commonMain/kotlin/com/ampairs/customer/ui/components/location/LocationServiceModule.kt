package com.ampairs.customer.ui.components.location

import org.koin.dsl.module

/**
 * Koin module for location services
 */
val locationServiceModule = module {
    single<LocationService> { createLocationService() }
}

/**
 * Platform-specific factory function
 */
expect fun createLocationService(): LocationService