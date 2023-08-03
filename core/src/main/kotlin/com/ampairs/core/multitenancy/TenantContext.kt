package com.ampairs.core.multitenancy

import com.ampairs.core.domain.model.Company

object TenantContext {
    private val currentTenant: ThreadLocal<Company> = InheritableThreadLocal()

    fun getCurrentTenant(): Company {
        return currentTenant.get()
    }

    fun setCurrentTenant(tenant: Company) {
        currentTenant.set(tenant)
    }

}
