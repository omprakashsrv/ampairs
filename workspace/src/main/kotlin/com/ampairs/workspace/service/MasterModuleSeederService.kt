package com.ampairs.workspace.service

import com.ampairs.workspace.model.MasterModule
import com.ampairs.workspace.model.enums.*
import com.ampairs.workspace.repository.MasterModuleRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service to seed initial master modules based on existing system modules.
 * Runs automatically on application startup to ensure catalog consistency.
 */
@Service
@Transactional
class MasterModuleSeederService(
    private val masterModuleRepository: MasterModuleRepository
) : CommandLineRunner {
    
    private val logger = LoggerFactory.getLogger(MasterModuleSeederService::class.java)
    
    override fun run(vararg args: String) {
        seedMasterModules()
    }
    
    fun seedMasterModules() {
        logger.info("Starting master module seeding process...")
        
        val existingModules = masterModuleRepository.findAll().associateBy { it.moduleCode }
        
        getSystemModules().forEach { moduleData ->
            val existingModule = existingModules[moduleData.moduleCode]
            if (existingModule == null) {
                logger.info("Seeding new master module: {}", moduleData.moduleCode)
                masterModuleRepository.save(moduleData)
            } else {
                logger.info("Updating existing master module: {}", moduleData.moduleCode)
                // Update existing module with all new data
                existingModule.apply {
                    name = moduleData.name
                    description = moduleData.description
                    tagline = moduleData.tagline
                    category = moduleData.category
                    status = moduleData.status
                    requiredTier = moduleData.requiredTier
                    requiredRole = moduleData.requiredRole
                    complexity = moduleData.complexity
                    version = moduleData.version
                    businessRelevance = moduleData.businessRelevance
                    configuration = moduleData.configuration
                    uiMetadata = moduleData.uiMetadata
                    routeInfo = moduleData.routeInfo
                    navigationIndex = moduleData.navigationIndex
                    provider = moduleData.provider
                    sizeMb = moduleData.sizeMb
                    featured = moduleData.featured
                    displayOrder = moduleData.displayOrder
                    active = moduleData.active
                }
                masterModuleRepository.save(existingModule)
            }
        }
        
        logger.info("Master module seeding completed")
    }
    
    private fun getSystemModules(): List<MasterModule> {
        return listOf(
            createBusinessModule(),
            createCustomerModule(),
            createProductModule(),
            createOrderModule(),
            createInvoiceModule(),
            createInventoryModule(),
            createTaxCodeModule(),
            createUnitModule()
        )
    }

    private fun createBusinessModule() = MasterModule().apply {
        moduleCode = "business-profile"
        name = "Business Management"
        description = "Comprehensive business configuration and management with company profile, registration details, operational settings, and tax compliance for complete business control"
        tagline = "Configure and manage your business operations"
        category = ModuleCategory.ADMINISTRATION
        status = ModuleStatus.ACTIVE
        requiredTier = SubscriptionTier.FREE
        requiredRole = UserRole.EMPLOYEE
        complexity = ModuleComplexity.ESSENTIAL
        version = "1.0.0"
        businessRelevance = listOf(
            createBusinessRelevance(BusinessType.RETAIL, 10, true, "Essential for managing store profile, operations, and tax compliance"),
            createBusinessRelevance(BusinessType.WHOLESALE, 10, true, "Critical for B2B business configuration and regulatory compliance"),
            createBusinessRelevance(BusinessType.MANUFACTURING, 10, true, "Mandatory for factory operations and business configuration")
        )
        configuration = createModuleConfiguration(
            requiredPermissions = listOf("BUSINESS_READ", "BUSINESS_WRITE"),
            optionalPermissions = listOf("BUSINESS_ADMIN", "TAX_CONFIG"),
            defaultEnabled = true
        )
        uiMetadata = createUIMetadata(
            icon = "business",
            primaryColor = "#673AB7",
            tags = listOf("Business Configuration", "Profile", "Operations", "Tax Compliance", "Settings")
        )
        routeInfo = createRouteInfo(
            basePath = "/business",
            displayName = "Business",
            iconName = "business",
            menuItems = listOf(
                createMenuItem("business-overview", "Overview", "/business/overview", "dashboard", 1, true),
                createMenuItem("business-profile", "Profile & Registration", "/business/profile", "business", 2),
                createMenuItem("business-operations", "Operations", "/business/operations", "settings", 3),
                createMenuItem("business-tax", "Tax Configuration", "/business/tax", "receipt_long", 4)
            )
        )
        navigationIndex = 5
        provider = "Ampairs"
        sizeMb = 3
        featured = true
        displayOrder = 5
        active = true
    }

    private fun createCustomerModule() = MasterModule().apply {
        moduleCode = "customer-management"
        name = "Customer Management"
        description = "Comprehensive customer relationship management with contact information, credit limits, GST compliance, and business relationship tracking"
        tagline = "Manage your business relationships effectively"
        category = ModuleCategory.CUSTOMER_MANAGEMENT
        status = ModuleStatus.ACTIVE
        requiredTier = SubscriptionTier.FREE
        requiredRole = UserRole.EMPLOYEE
        complexity = ModuleComplexity.STANDARD
        version = "1.0.0"
        businessRelevance = listOf(
            createBusinessRelevance(BusinessType.RETAIL, 9, true, "Essential for managing customer database and relationships"),
            createBusinessRelevance(BusinessType.WHOLESALE, 10, true, "Critical for B2B customer management and credit control"),
            createBusinessRelevance(BusinessType.MANUFACTURING, 8, true, "Important for managing distributor and dealer networks")
        )
        configuration = createModuleConfiguration(
            requiredPermissions = listOf("CUSTOMER_READ", "CUSTOMER_WRITE"),
            optionalPermissions = listOf("CUSTOMER_DELETE", "CUSTOMER_EXPORT")
        )
        uiMetadata = createUIMetadata(
            icon = "people",
            primaryColor = "#1976D2",
            tags = listOf("CRM", "Contacts", "GST", "Credit Management")
        )
        routeInfo = createRouteInfo(
            basePath = "/customers",
            displayName = "Customers",
            iconName = "people",
            menuItems = listOf(
                createMenuItem("customer-list", "All Customers", "/customers", "people", 1, true),
                createMenuItem("customer-create", "Create Customer", "/customers/create", "person_add", 2),
//                createMenuItem("customer-import", "Import Customers", "/customers/import", "upload", 3),
                createMenuItem("customer-states", "Manage States", "/customers/states", "location_on", 4),
                createMenuItem("customer-types", "Customer Types", "/customers/types", "category", 5),
                createMenuItem("customer-groups", "Customer Groups", "/customers/groups", "group", 6),
                createMenuItem("customer-config", "Configuration", "/customers/config", "tune", 7),
            )
        )
        navigationIndex = 20
        provider = "Ampairs"
        sizeMb = 5
        featured = true
        displayOrder = 10
        active = true
    }
    
    private fun createProductModule() = MasterModule().apply {
        moduleCode = "product-management"
        name = "Product Catalog"
        description = "Complete product management system with inventory tracking, pricing, tax codes, and category organization for comprehensive catalog management"
        tagline = "Organize and manage your entire product inventory"
        category = ModuleCategory.INVENTORY_MANAGEMENT
        status = ModuleStatus.ACTIVE
        requiredTier = SubscriptionTier.FREE
        requiredRole = UserRole.EMPLOYEE
        complexity = ModuleComplexity.STANDARD
        version = "1.0.0"
        businessRelevance = listOf(
            createBusinessRelevance(BusinessType.RETAIL, 10, true, "Core module for managing product inventory and pricing"),
            createBusinessRelevance(BusinessType.WHOLESALE, 10, true, "Essential for bulk product management and pricing tiers"),
            createBusinessRelevance(BusinessType.MANUFACTURING, 9, true, "Critical for managing raw materials and finished goods")
        )
        configuration = createModuleConfiguration(
            requiredPermissions = listOf("PRODUCT_READ", "PRODUCT_WRITE"),
            optionalPermissions = listOf("PRODUCT_DELETE", "INVENTORY_MANAGEMENT"),
            dependencies = listOf("tax-code-management")
        )
        uiMetadata = createUIMetadata(
            icon = "inventory",
            primaryColor = "#388E3C",
            tags = listOf("Inventory", "Catalog", "Pricing", "Stock Management")
        )
        routeInfo = createRouteInfo(
            basePath = "/products",
            displayName = "Products",
            iconName = "inventory",
            menuItems = listOf(
                createMenuItem("product-list", "All Products", "/products", "inventory", 1, true),
                createMenuItem("product-create", "Create Product", "/products/create", "add", 2),
                createMenuItem("product-categories", "Categories", "/products/categories", "category", 3)
            )
        )
        navigationIndex = 30
        provider = "Ampairs"
        sizeMb = 8
        featured = true
        displayOrder = 20
        active = true
    }
    
    private fun createOrderModule() = MasterModule().apply {
        moduleCode = "order-management"
        name = "Order Management"
        description = "End-to-end order processing system with workflow management, status tracking, and integration with inventory and invoicing systems"
        tagline = "Streamline your sales order processing"
        category = ModuleCategory.SALES_MANAGEMENT
        status = ModuleStatus.ACTIVE
        requiredTier = SubscriptionTier.BASIC
        requiredRole = UserRole.EMPLOYEE
        complexity = ModuleComplexity.STANDARD
        version = "1.0.0"
        businessRelevance = listOf(
            createBusinessRelevance(BusinessType.RETAIL, 9, true, "Essential for managing sales transactions and order fulfillment"),
            createBusinessRelevance(BusinessType.WHOLESALE, 10, true, "Critical for B2B order processing and bulk sales management"),
            createBusinessRelevance(BusinessType.MANUFACTURING, 8, false, "Important for managing production orders and delivery")
        )
        configuration = createModuleConfiguration(
            requiredPermissions = listOf("ORDER_READ", "ORDER_WRITE"),
            optionalPermissions = listOf("ORDER_DELETE", "ORDER_APPROVE"),
            dependencies = listOf("customer-management", "product-management")
        )
        uiMetadata = createUIMetadata(
            icon = "shopping_cart",
            primaryColor = "#F57C00",
            tags = listOf("Sales", "Orders", "Workflow", "Processing")
        )
        routeInfo = createRouteInfo(
            basePath = "/orders",
            displayName = "Orders",
            iconName = "shopping_cart",
            menuItems = listOf(
                createMenuItem("order-list", "All Orders", "/orders", "shopping_cart", 1, true),
                createMenuItem("order-create", "Create Order", "/orders/create", "add_shopping_cart", 2),
                createMenuItem("order-drafts", "Draft Orders", "/orders/drafts", "drafts", 3)
            )
        )
        navigationIndex = 40
        provider = "Ampairs"
        sizeMb = 12
        featured = true
        displayOrder = 30
        active = true
    }
    
    private fun createInvoiceModule() = MasterModule().apply {
        moduleCode = "invoice-billing"
        name = "Invoice & Billing"
        description = "Comprehensive invoicing system with GST compliance, payment tracking, PDF generation, and automated billing workflows"
        tagline = "Generate professional invoices with GST compliance"
        category = ModuleCategory.FINANCIAL_MANAGEMENT
        status = ModuleStatus.ACTIVE
        requiredTier = SubscriptionTier.BASIC
        requiredRole = UserRole.EMPLOYEE
        complexity = ModuleComplexity.ADVANCED
        version = "1.0.0"
        businessRelevance = listOf(
            createBusinessRelevance(BusinessType.RETAIL, 10, true, "Essential for billing customers and GST compliance"),
            createBusinessRelevance(BusinessType.WHOLESALE, 10, true, "Critical for B2B invoicing and payment management"),
            createBusinessRelevance(BusinessType.MANUFACTURING, 9, true, "Important for billing distributors and managing receivables")
        )
        configuration = createModuleConfiguration(
            requiredPermissions = listOf("INVOICE_READ", "INVOICE_WRITE"),
            optionalPermissions = listOf("INVOICE_DELETE", "PAYMENT_RECONCILE"),
            dependencies = listOf("customer-management", "order-management", "tax-code-management")
        )
        uiMetadata = createUIMetadata(
            icon = "receipt",
            primaryColor = "#7B1FA2",
            tags = listOf("Invoicing", "GST", "Billing", "Payments", "PDF")
        )
        routeInfo = createRouteInfo(
            basePath = "/invoices",
            displayName = "Sales",
            iconName = "receipt",
            menuItems = listOf(
                createMenuItem("invoice-list", "All Invoices", "/invoices", "receipt", 1, true),
                createMenuItem("invoice-create", "Create Invoice", "/invoices/create", "add", 2)
            )
        )
        navigationIndex = 50
        provider = "Ampairs"
        sizeMb = 15
        featured = true
        displayOrder = 40
        active = true
    }

    private fun createInventoryModule() = MasterModule().apply {
        moduleCode = "inventory-management"
        name = "Inventory Management"
        description = "Advanced inventory control with stock tracking, low stock alerts, batch management, and multi-location inventory support"
        tagline = "Keep perfect track of your stock levels"
        category = ModuleCategory.INVENTORY_MANAGEMENT
        status = ModuleStatus.ACTIVE
        requiredTier = SubscriptionTier.PREMIUM
        requiredRole = UserRole.MANAGER
        complexity = ModuleComplexity.ADVANCED
        version = "1.0.0"
        businessRelevance = listOf(
            createBusinessRelevance(BusinessType.RETAIL, 8, false, "Useful for advanced stock management and reorder automation"),
            createBusinessRelevance(BusinessType.WHOLESALE, 9, true, "Critical for managing large inventory volumes and locations"),
            createBusinessRelevance(BusinessType.MANUFACTURING, 10, true, "Essential for raw material and finished goods tracking")
        )
        configuration = createModuleConfiguration(
            requiredPermissions = listOf("INVENTORY_READ", "INVENTORY_WRITE"),
            optionalPermissions = listOf("INVENTORY_ADJUST", "BATCH_MANAGEMENT"),
            dependencies = listOf("product-management")
        )
        uiMetadata = createUIMetadata(
            icon = "warehouse",
            primaryColor = "#5D4037",
            tags = listOf("Stock Control", "Warehousing", "Alerts", "Multi-location")
        )
        routeInfo = createRouteInfo(
            basePath = "/inventory",
            displayName = "Inventory",
            iconName = "warehouse",
            menuItems = listOf(
                createMenuItem("inventory-list", "Inventory", "/inventory", "warehouse", 1, true)
            )
        )
        navigationIndex = 60
        provider = "Ampairs"
        sizeMb = 20
        featured = false
        displayOrder = 50
        active = true
    }

    private fun createUnitModule() = MasterModule().apply {
        moduleCode = "unit-management"
        name = "Unit of Measure"
        description = "Manage units of measurement and conversion rules for products — supports weight, volume, length, count, and custom units with automatic conversion calculations"
        tagline = "Define and convert units across your product catalog"
        category = ModuleCategory.INVENTORY_MANAGEMENT
        status = ModuleStatus.ACTIVE
        requiredTier = SubscriptionTier.FREE
        requiredRole = UserRole.EMPLOYEE
        complexity = ModuleComplexity.STANDARD
        version = "1.0.0"
        businessRelevance = listOf(
            createBusinessRelevance(BusinessType.RETAIL, 8, true, "Essential for selling products in multiple units (kg, g, piece, pack)"),
            createBusinessRelevance(BusinessType.WHOLESALE, 9, true, "Critical for bulk quantity management and unit conversions"),
            createBusinessRelevance(BusinessType.MANUFACTURING, 10, true, "Mandatory for raw material and production unit tracking")
        )
        configuration = createModuleConfiguration(
            requiredPermissions = listOf("UNIT_READ", "UNIT_WRITE"),
            optionalPermissions = listOf("UNIT_DELETE"),
            dependencies = listOf("product-management")
        )
        uiMetadata = createUIMetadata(
            icon = "straighten",
            primaryColor = "#0097A7",
            tags = listOf("Units", "Conversions", "Measurement", "Weight", "Volume")
        )
        routeInfo = createRouteInfo(
            basePath = "/units",
            displayName = "Units",
            iconName = "straighten",
            menuItems = listOf(
                createMenuItem("unit-list", "All Units", "/units", "straighten", 1, true),
                createMenuItem("unit-conversions", "Conversions", "/units/conversions", "swap_horiz", 2)
            )
        )
        navigationIndex = 65
        provider = "Ampairs"
        sizeMb = 3
        featured = false
        displayOrder = 55
        active = true
    }

    private fun createTaxCodeModule() = MasterModule().apply {
        moduleCode = "tax-code-management"
        name = "Tax Code Management"
        description = "GST compliance system with automatic tax calculations, SGST/CGST/IGST handling, and HSN code management for Indian taxation"
        tagline = "Stay GST compliant with automated tax calculations"
        category = ModuleCategory.FINANCIAL_MANAGEMENT
        status = ModuleStatus.ACTIVE
        requiredTier = SubscriptionTier.FREE
        requiredRole = UserRole.EMPLOYEE
        complexity = ModuleComplexity.STANDARD
        version = "1.0.0"
        businessRelevance = listOf(
            createBusinessRelevance(BusinessType.RETAIL, 10, true, "Mandatory for GST compliance in retail operations"),
            createBusinessRelevance(BusinessType.WHOLESALE, 10, true, "Essential for B2B GST compliance and tax calculations"),
            createBusinessRelevance(BusinessType.MANUFACTURING, 10, true, "Critical for manufacturing GST compliance and input tax credit")
        )
        configuration = createModuleConfiguration(
            requiredPermissions = listOf("TAX_CODE_READ", "TAX_CODE_WRITE"),
            optionalPermissions = listOf("TAX_REPORTS", "GST_RETURNS")
        )
        uiMetadata = createUIMetadata(
            icon = "calculate",
            primaryColor = "#D32F2F",
            tags = listOf("GST", "Tax Compliance", "HSN Codes", "Indian Taxation")
        )
        routeInfo = createRouteInfo(
            basePath = "/tax",
            displayName = "Tax",
            iconName = "calculate",
            menuItems = listOf(
                createMenuItem("tax-list", "Tax Codes", "/tax", "calculate", 1, true),
                createMenuItem("tax-calculator", "Calculator", "/tax/calculator", "calculate", 2)
            )
        )
        navigationIndex = 70
        provider = "Ampairs"
        sizeMb = 6
        featured = true
        displayOrder = 60
        active = true
    }
    
    // Helper functions to create embedded objects
    private fun createBusinessRelevance(
        businessType: BusinessType,
        relevanceScore: Int,
        isEssential: Boolean,
        recommendationReason: String
    ) = com.ampairs.workspace.model.BusinessRelevance(
        businessType = businessType,
        relevanceScore = relevanceScore,
        isEssential = isEssential,
        recommendationReason = recommendationReason
    )
    
    private fun createModuleConfiguration(
        requiredPermissions: List<String> = emptyList(),
        optionalPermissions: List<String> = emptyList(),
        defaultEnabled: Boolean = true,
        dependencies: List<String> = emptyList(),
        conflictsWith: List<String> = emptyList(),
        customSettings: Map<String, Any> = emptyMap()
    ) = com.ampairs.workspace.model.ModuleConfiguration(
        requiredPermissions = requiredPermissions,
        optionalPermissions = optionalPermissions,
        defaultEnabled = defaultEnabled,
        dependencies = dependencies,
        conflictsWith = conflictsWith,
        customSettings = customSettings
    )
    
    private fun createUIMetadata(
        icon: String,
        primaryColor: String,
        secondaryColor: String? = null,
        backgroundColor: String? = null,
        screenshots: List<String> = emptyList(),
        bannerImage: String? = null,
        demoUrl: String? = null,
        videoUrl: String? = null,
        tags: List<String> = emptyList(),
        keywords: List<String> = emptyList()
    ) = com.ampairs.workspace.model.ModuleUIMetadata(
        icon = icon,
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        backgroundColor = backgroundColor,
        screenshots = screenshots,
        bannerImage = bannerImage,
        demoUrl = demoUrl,
        videoUrl = videoUrl,
        tags = tags,
        keywords = keywords
    )

    private fun createRouteInfo(
        basePath: String,
        displayName: String,
        iconName: String,
        menuItems: List<com.ampairs.workspace.model.ModuleMenuItem> = emptyList()
    ) = com.ampairs.workspace.model.ModuleRouteInfo(
        basePath = basePath,
        displayName = displayName,
        iconName = iconName,
        menuItems = menuItems
    )

    private fun createMenuItem(
        id: String,
        label: String,
        routePath: String,
        icon: String,
        order: Int,
        isDefault: Boolean = false
    ) = com.ampairs.workspace.model.ModuleMenuItem(
        id = id,
        label = label,
        routePath = routePath,
        icon = icon,
        order = order,
        isDefault = isDefault
    )
}