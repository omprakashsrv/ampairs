package com.ampairs.core.multitenancy

import org.hibernate.cfg.AvailableSettings
import org.hibernate.context.spi.CurrentTenantIdentifierResolver
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.stereotype.Component


@Component
class TenantIdentifierResolver : CurrentTenantIdentifierResolver, HibernatePropertiesCustomizer {

    override fun resolveCurrentTenantIdentifier(): String {
        return TenantContext.getCurrentTenant() ?: ""
    }

    override fun validateExistingCurrentSessions(): Boolean {
        return true
    }

    override fun customize(hibernateProperties: MutableMap<String, Any>) {
        hibernateProperties[AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER] = this
    }
}