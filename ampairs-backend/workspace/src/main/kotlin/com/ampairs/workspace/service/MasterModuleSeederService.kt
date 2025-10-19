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
    
    override fun run(vararg args: String?) {
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
            createNotificationModule(),
            createUserManagementModule(),
            createReportingModule(),
            createDashboardModule()
        )
    }

    private fun createBusinessModule() = MasterModule().apply {
        moduleCode = "business-profile"
        name = "Business Profile"
        description = "Core business profile management with company information, branding, operational settings, and multi-timezone support for comprehensive business configuration"
        tagline = "Configure your business identity and operations"
        category = ModuleCategory.ADMINISTRATION
        status = ModuleStatus.ACTIVE
        requiredTier = SubscriptionTier.FREE
        requiredRole = UserRole.EMPLOYEE
        complexity = ModuleComplexity.ESSENTIAL
        version = "1.0.0"
        businessRelevance = listOf(
            createBusinessRelevance(BusinessType.RETAIL, 10, true, "Essential for setting up store identity and operational parameters"),
            createBusinessRelevance(BusinessType.WHOLESALE, 10, true, "Critical for B2B business identity and regulatory compliance"),
            createBusinessRelevance(BusinessType.MANUFACTURING, 10, true, "Mandatory for factory settings and business operations configuration")
        )
        configuration = createModuleConfiguration(
            requiredPermissions = listOf("BUSINESS_READ", "BUSINESS_WRITE"),
            optionalPermissions = listOf("BUSINESS_ADMIN", "BUSINESS_SETTINGS"),
            defaultEnabled = true
        )
        uiMetadata = createUIMetadata(
            icon = "store",
            primaryColor = "#673AB7",
            tags = listOf("Business Setup", "Profile", "Configuration", "Branding", "Operations")
        )
        routeInfo = createRouteInfo(
            basePath = "/business",
            displayName = "Business",
            iconName = "store",
            menuItems = listOf(
                createMenuItem("business-profile", "Business Profile", "/business/profile", "store", 1, true),
                createMenuItem("business-settings", "Settings", "/business/settings", "settings", 2),
                createMenuItem("business-branding", "Branding", "/business/branding", "palette", 3)
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
            dependencies = listOf("customer-management", "product-catalog")
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
            dependencies = listOf("product-catalog")
        )
        uiMetadata = createUIMetadata(
            icon = "warehouse",
            primaryColor = "#5D4037",
            tags = listOf("Stock Control", "Warehousing", "Alerts", "Multi-location")
        )
        provider = "Ampairs"
        sizeMb = 20
        featured = false
        displayOrder = 50
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
        provider = "Ampairs"
        sizeMb = 6
        featured = true
        displayOrder = 60
        active = true
    }
    
    private fun createNotificationModule() = MasterModule().apply {
        moduleCode = "notification-system"
        name = "Notification System"
        description = "Comprehensive notification management with email, SMS, and in-app notifications for business events and alerts"
        tagline = "Stay informed with intelligent notifications"
        category = ModuleCategory.COMMUNICATION
        status = ModuleStatus.ACTIVE
        requiredTier = SubscriptionTier.BASIC
        requiredRole = UserRole.EMPLOYEE
        complexity = ModuleComplexity.STANDARD
        version = "1.0.0"
        businessRelevance = listOf(
            createBusinessRelevance(BusinessType.RETAIL, 7, false, "Helpful for customer communication and alerts"),
            createBusinessRelevance(BusinessType.WHOLESALE, 8, false, "Useful for B2B communication and order updates"),
            createBusinessRelevance(BusinessType.MANUFACTURING, 8, false, "Important for production alerts and status updates")
        )
        configuration = createModuleConfiguration(
            requiredPermissions = listOf("NOTIFICATION_READ"),
            optionalPermissions = listOf("NOTIFICATION_SEND", "NOTIFICATION_ADMIN")
        )
        uiMetadata = createUIMetadata(
            icon = "notifications",
            primaryColor = "#FF5722",
            tags = listOf("Alerts", "Email", "SMS", "Communication")
        )
        provider = "Ampairs"
        sizeMb = 4
        featured = false
        displayOrder = 70
        active = true
    }
    
    private fun createUserManagementModule() = MasterModule().apply {
        moduleCode = "user-management"
        name = "User Management"
        description = "Complete user administration with role-based access control, permissions, and multi-device authentication support"
        tagline = "Manage users and permissions effectively"
        category = ModuleCategory.ADMINISTRATION
        status = ModuleStatus.ACTIVE
        requiredTier = SubscriptionTier.FREE
        requiredRole = UserRole.ADMIN
        complexity = ModuleComplexity.ADVANCED
        version = "1.0.0"
        businessRelevance = listOf(
            createBusinessRelevance(BusinessType.RETAIL, 8, true, "Important for managing staff access and permissions"),
            createBusinessRelevance(BusinessType.WHOLESALE, 9, true, "Critical for managing multiple user roles and access levels"),
            createBusinessRelevance(BusinessType.MANUFACTURING, 9, true, "Essential for managing diverse workforce access requirements")
        )
        configuration = createModuleConfiguration(
            requiredPermissions = listOf("USER_READ", "USER_WRITE"),
            optionalPermissions = listOf("ROLE_MANAGEMENT", "PERMISSION_ADMIN")
        )
        uiMetadata = createUIMetadata(
            icon = "admin_panel_settings",
            primaryColor = "#424242",
            tags = listOf("Users", "Roles", "Permissions", "Security")
        )
        provider = "Ampairs"
        sizeMb = 10
        featured = false
        displayOrder = 80
        active = true
    }
    
    private fun createReportingModule() = MasterModule().apply {
        moduleCode = "business-reporting"
        name = "Business Reporting"
        description = "Advanced reporting and analytics with customizable dashboards, financial reports, and business intelligence insights"
        tagline = "Transform data into actionable business insights"
        category = ModuleCategory.ANALYTICS_REPORTING
        status = ModuleStatus.ACTIVE
        requiredTier = SubscriptionTier.PREMIUM
        requiredRole = UserRole.MANAGER
        complexity = ModuleComplexity.SPECIALIZED
        version = "1.0.0"
        businessRelevance = listOf(
            createBusinessRelevance(BusinessType.RETAIL, 9, false, "Valuable for sales analysis and business performance tracking"),
            createBusinessRelevance(BusinessType.WHOLESALE, 10, true, "Critical for B2B analytics and performance monitoring"),
            createBusinessRelevance(BusinessType.MANUFACTURING, 9, true, "Essential for production analytics and efficiency metrics")
        )
        configuration = createModuleConfiguration(
            requiredPermissions = listOf("REPORT_READ", "REPORT_GENERATE"),
            optionalPermissions = listOf("REPORT_ADMIN", "DASHBOARD_CONFIG"),
            dependencies = listOf("customer-management", "product-catalog", "order-management", "invoice-billing")
        )
        uiMetadata = createUIMetadata(
            icon = "analytics",
            primaryColor = "#3F51B5",
            tags = listOf("Reports", "Analytics", "Business Intelligence", "Dashboards")
        )
        provider = "Ampairs"
        sizeMb = 25
        featured = true
        displayOrder = 90
        active = true
    }
    
    private fun createDashboardModule() = MasterModule().apply {
        moduleCode = "business-dashboard"
        name = "Business Dashboard"
        description = "Executive dashboard with real-time KPIs, performance metrics, and customizable widgets for business monitoring"
        tagline = "Get instant insights into your business performance"
        category = ModuleCategory.ANALYTICS_REPORTING
        status = ModuleStatus.ACTIVE
        requiredTier = SubscriptionTier.FREE
        requiredRole = UserRole.EMPLOYEE
        complexity = ModuleComplexity.ESSENTIAL
        version = "1.0.0"
        businessRelevance = listOf(
            createBusinessRelevance(BusinessType.RETAIL, 8, false, "Helpful for monitoring daily sales and performance"),
            createBusinessRelevance(BusinessType.WHOLESALE, 9, false, "Valuable for tracking B2B metrics and trends"),
            createBusinessRelevance(BusinessType.MANUFACTURING, 8, false, "Useful for production monitoring and efficiency tracking")
        )
        configuration = createModuleConfiguration(
            requiredPermissions = listOf("DASHBOARD_READ"),
            optionalPermissions = listOf("DASHBOARD_CONFIG")
        )
        uiMetadata = createUIMetadata(
            icon = "dashboard",
            primaryColor = "#00BCD4",
            tags = listOf("Dashboard", "KPIs", "Monitoring", "Widgets")
        )
        routeInfo = createRouteInfo(
            basePath = "/dashboard",
            displayName = "Dashboard",
            iconName = "dashboard",
            menuItems = listOf(
                createMenuItem("main-dashboard", "Overview", "/dashboard", "dashboard", 1, true),
                createMenuItem("dashboard-config", "Customize", "/dashboard/configure", "tune", 2)
            )
        )
        navigationIndex = 10
        provider = "Ampairs"
        sizeMb = 8
        featured = false
        displayOrder = 100
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