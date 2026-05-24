package com.ampairs.core.multitenancy

import org.hibernate.cfg.AvailableSettings
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MultiTenancyConfiguration(
    private val currentTenantIdentifierResolver: CurrentTenantIdentifierResolver
) {

    @Bean
    fun hibernatePropertiesCustomizer(): HibernatePropertiesCustomizer {
        return HibernatePropertiesCustomizer { hibernateProperties ->
            hibernateProperties[AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER] =
                currentTenantIdentifierResolver
        }
    }
}