package com.ampairs.core.multitenancy


object TenantContext {
    private val currentTenant: ThreadLocal<String?> = InheritableThreadLocal()

    fun getCurrentTenant(): String? {
        return currentTenant.get()
    }

    fun setCurrentTenant(tenant: String) {
        currentTenant.set(tenant)
    }

}
