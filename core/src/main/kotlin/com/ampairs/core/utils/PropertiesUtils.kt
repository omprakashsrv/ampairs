package com.ampairs.core.utils

import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.io.ClassPathResource


object PropertiesUtils {
    const val SPRING_PROFILES_ACTIVE = "spring.profiles.active"
    fun initProperties() {
        var activeProfile = System.getProperty(SPRING_PROFILES_ACTIVE)
        if (activeProfile == null) {
            activeProfile = "dev"
        }
        val propertySourcesPlaceholderConfigurer = PropertySourcesPlaceholderConfigurer()
        propertySourcesPlaceholderConfigurer.setLocations(
            ClassPathResource("application-$activeProfile.yml")
        )
    }
}