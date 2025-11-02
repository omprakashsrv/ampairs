package com.ampairs.core.multitenancy

import org.hibernate.cfg.AvailableSettings
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring Configuration for Hibernate Multi-Tenancy using @TenantId approach
 * This is the simplest and most reliable way to implement multi-tenancy in Spring Boot
 */
@Configuration
class MultiTenancyConfiguration {

    @Autowired
    private lateinit var currentTenantIdentifierResolver: CurrentTenantIdentifierResolver

    /**
     * Configure Hibernate to use our tenant identifier resolver
     * This enables @TenantId annotation support
     */
    @Bean
    fun hibernatePropertiesCustomizer(): HibernatePropertiesCustomizer {
        return HibernatePropertiesCustomizer { hibernateProperties ->
            // Register our tenant identifier resolver
            // This provides tenant values for @TenantId annotated fields
            hibernateProperties[AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER] =
                currentTenantIdentifierResolver
        }
    }
}