package com.ampairs.workspace.navigation

import Route
import CustomerRoute
import ProductRoute
import OrderRoute
import InvoiceRoute

/**
 * Initialize and register all module providers
 * This function should be called during app initialization
 */
fun initializeModuleProviders() {
    ModuleRegistry.register(CustomerModuleProvider)
    ModuleRegistry.register(ProductModuleProvider)
    ModuleRegistry.register(OrderModuleProvider)
    ModuleRegistry.register(InvoiceModuleProvider)
    ModuleRegistry.register(TaxModuleProvider)
    ModuleRegistry.register(TaxCodeModuleProvider)
}

/**
 * Customer module navigation provider
 * Maps "customer-management" module code to customer navigation routes
 * Includes menu item navigation for types and groups
 */
object CustomerModuleProvider : IModuleNavigationProvider {
    override val moduleCode: String = "customer-management"
    override val displayName: String = "Customer Management"
    override val defaultRoute: Any = Route.Customer

    override fun getFeatureRoute(feature: String): Any? {
        return when (feature) {
            "customers", "list" -> Route.Customer
            "add", "create" -> CustomerRoute.CustomerEdit("")
            else -> null
        }
    }
}

/**
 * Product module navigation provider
 * Maps "product-management" module code to product navigation routes
 */
object ProductModuleProvider : IModuleNavigationProvider {
    override val moduleCode: String = "product-management"
    override val displayName: String = "Product Management"
    override val defaultRoute: Any = Route.Product

    override fun getFeatureRoute(feature: String): Any? {
        return when (feature) {
            "products", "list" -> ProductRoute.Products
            "groups", "categories" -> ProductRoute.Group()
            "tax-codes" -> ProductRoute.TaxCode
            "tax-info" -> ProductRoute.TaxInfo
            else -> null
        }
    }
}

/**
 * Order module navigation provider
 * Maps "order-management" module code to order navigation routes
 */
object OrderModuleProvider : IModuleNavigationProvider {
    override val moduleCode: String = "order-management"
    override val displayName: String = "Order Management"
    override val defaultRoute: Any = Route.Order

    override fun getFeatureRoute(feature: String): Any? {
        return when (feature) {
            "orders", "list" -> OrderRoute.Orders
            "create", "add" -> OrderRoute.Root()
            else -> null
        }
    }
}

/**
 * Invoice module navigation provider
 * Maps "invoice-management" module code to invoice navigation routes
 */
object InvoiceModuleProvider : IModuleNavigationProvider {
    override val moduleCode: String = "invoice-management"
    override val displayName: String = "Invoice Management"
    override val defaultRoute: Any = Route.Invoice

    override fun getFeatureRoute(feature: String): Any? {
        return when (feature) {
            "invoices", "list" -> InvoiceRoute.Invoices
            "create", "add" -> InvoiceRoute.Root()
            else -> null
        }
    }
}

/**
 * Tax module navigation provider
 * Maps "tax-management" and "tax-code-management" module codes to tax navigation routes
 */
object TaxModuleProvider : IModuleNavigationProvider {
    override val moduleCode: String = "tax-management"
    override val displayName: String = "Tax Management"
    override val defaultRoute: Any = Route.Tax

    override fun getFeatureRoute(feature: String): Any? {
        return when (feature) {
            "hsn-codes", "hsn" -> com.ampairs.tax.ui.navigation.HsnCodesListRoute
            "tax-calculator", "calculator" -> com.ampairs.tax.ui.navigation.TaxCalculatorRoute
            "tax-rates", "rates" -> com.ampairs.tax.ui.navigation.TaxRatesRoute
            else -> null
        }
    }
}

/**
 * Tax Code module navigation provider
 * Maps "tax-code-management" module code to tax navigation routes
 * This is a separate provider for the backend's "tax-code-management" module
 */
object TaxCodeModuleProvider : IModuleNavigationProvider {
    override val moduleCode: String = "tax-code-management"
    override val displayName: String = "Tax Code Management"
    override val defaultRoute: Any = Route.Tax

    override fun getFeatureRoute(feature: String): Any? {
        return when (feature) {
            "hsn-codes", "hsn" -> com.ampairs.tax.ui.navigation.HsnCodesListRoute
            "tax-calculator", "calculator" -> com.ampairs.tax.ui.navigation.TaxCalculatorRoute
            "tax-rates", "rates" -> com.ampairs.tax.ui.navigation.TaxRatesRoute
            else -> null
        }
    }
}

/**
 * Inventory module navigation provider
 * Maps "inventory-management" module code to inventory navigation routes
 * Note: Currently commented out as inventory module might not be fully implemented
 */
/*
object InventoryModuleProvider : IModuleNavigationProvider {
    override val moduleCode: String = "inventory-management"
    override val displayName: String = "Inventory Management"
    override val defaultRoute: Any = Route.Inventory

    override fun isAvailable(): Boolean {
        // Check if inventory module is implemented
        return try {
            // This will fail if inventory routes are not available
            Route.Inventory
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getFeatureRoute(feature: String): Any? {
        return when (feature) {
            "inventory", "stock" -> Route.Inventory
            else -> null
        }
    }
}
*/