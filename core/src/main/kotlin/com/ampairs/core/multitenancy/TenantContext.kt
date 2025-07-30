package com.ampairs.core.multitenancy

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TenantContext {
    companion object {
        private val logger = LoggerFactory.getLogger(TenantContext::class.java)
        private val currentTenant: ThreadLocal<String?> = InheritableThreadLocal()
        private val tenantStack: ThreadLocal<MutableList<String>> = ThreadLocal.withInitial { mutableListOf() }

        fun getCurrentTenant(): String? {
            return currentTenant.get()
        }

        fun setCurrentTenant(tenant: String?) {
            val previousTenant = currentTenant.get()
            if (tenant != null) {
                currentTenant.set(tenant)
                tenantStack.get().add(tenant)
                logger.debug("Tenant context set: {} (previous: {})", tenant, previousTenant)
            } else {
                currentTenant.remove()
                tenantStack.get().clear()
                logger.debug("Tenant context cleared (previous: {})", previousTenant)
            }
        }

        fun pushTenant(tenant: String) {
            val currentStack = tenantStack.get()
            currentStack.add(getCurrentTenant() ?: "")
            setCurrentTenant(tenant)
            logger.debug("Tenant pushed: {} (stack size: {})", tenant, currentStack.size)
        }

        fun popTenant(): String? {
            val currentStack = tenantStack.get()
            val previousTenant = if (currentStack.isNotEmpty()) {
                currentStack.removeLastOrNull()
            } else null

            setCurrentTenant(previousTenant)
            logger.debug("Tenant popped: {} (stack size: {})", previousTenant, currentStack.size)
            return previousTenant
        }

        fun clearTenantContext() {
            currentTenant.remove()
            tenantStack.get().clear()
            logger.debug("Tenant context completely cleared")
        }

        fun requireCurrentTenant(): String {
            return getCurrentTenant()
                ?: throw IllegalStateException("No tenant context available in current thread")
        }

        fun <T> withTenant(tenantId: String, block: () -> T): T {
            val originalTenant = getCurrentTenant()
            return try {
                setCurrentTenant(tenantId)
                block()
            } finally {
                setCurrentTenant(originalTenant)
            }
        }
    }
}
