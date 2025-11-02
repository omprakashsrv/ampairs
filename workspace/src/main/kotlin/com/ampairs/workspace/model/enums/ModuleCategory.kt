package com.ampairs.workspace.model.enums

/**
 * Categories for organizing business modules
 */
enum class ModuleCategory(
    val displayName: String,
    val description: String,
    val icon: String,
    val displayOrder: Int
) {
    CUSTOMER_MANAGEMENT("Customer Management", "CRM, contacts, and customer relationships", "people", 1),
    SALES_MANAGEMENT("Sales Management", "Sales pipeline, leads, and opportunities", "trending_up", 2),
    FINANCIAL_MANAGEMENT("Financial Management", "Invoicing, payments, and accounting", "account_balance", 3),
    INVENTORY_MANAGEMENT("Inventory Management", "Products, stock, and warehouse management", "inventory_2", 4),
    ORDER_MANAGEMENT("Order Management", "Sales and purchase order processing", "shopping_cart", 5),
    ANALYTICS_REPORTING("Analytics & Reporting", "Business intelligence and insights", "analytics", 6),
    COMMUNICATION("Communication", "Messaging, notifications, and team collaboration", "chat", 7),
    PROJECT_MANAGEMENT("Project Management", "Tasks, projects, and workflow management", "work", 8),
    HR_MANAGEMENT("HR Management", "Employee management and HR processes", "badge", 9),
    MARKETING("Marketing", "Campaigns, promotions, and customer engagement", "campaign", 10),
    INTEGRATIONS("Integrations", "Third-party services and API connections", "sync", 11),
    ADMINISTRATION("Administration", "System settings and configuration", "settings", 12)
}

/**
 * Business types that help filter relevant modules
 */
enum class BusinessType(
    val displayName: String,
    val description: String,
    val relevantModules: List<ModuleCategory>
) {
    RETAIL(
        "Retail & E-commerce",
        "Online and physical retail businesses",
        listOf(
            ModuleCategory.CUSTOMER_MANAGEMENT,
            ModuleCategory.SALES_MANAGEMENT,
            ModuleCategory.INVENTORY_MANAGEMENT,
            ModuleCategory.ORDER_MANAGEMENT,
            ModuleCategory.FINANCIAL_MANAGEMENT,
            ModuleCategory.MARKETING,
            ModuleCategory.ANALYTICS_REPORTING
        )
    ),

    SERVICES(
        "Service Business",
        "Professional services, consulting, agencies",
        listOf(
            ModuleCategory.CUSTOMER_MANAGEMENT,
            ModuleCategory.PROJECT_MANAGEMENT,
            ModuleCategory.FINANCIAL_MANAGEMENT,
            ModuleCategory.COMMUNICATION,
            ModuleCategory.ANALYTICS_REPORTING,
            ModuleCategory.HR_MANAGEMENT
        )
    ),

    MANUFACTURING(
        "Manufacturing",
        "Production and manufacturing businesses",
        listOf(
            ModuleCategory.INVENTORY_MANAGEMENT,
            ModuleCategory.ORDER_MANAGEMENT,
            ModuleCategory.CUSTOMER_MANAGEMENT,
            ModuleCategory.FINANCIAL_MANAGEMENT,
            ModuleCategory.PROJECT_MANAGEMENT,
            ModuleCategory.ANALYTICS_REPORTING,
            ModuleCategory.HR_MANAGEMENT
        )
    ),

    WHOLESALE(
        "Wholesale & Distribution",
        "B2B wholesale and distribution companies",
        listOf(
            ModuleCategory.CUSTOMER_MANAGEMENT,
            ModuleCategory.SALES_MANAGEMENT,
            ModuleCategory.INVENTORY_MANAGEMENT,
            ModuleCategory.ORDER_MANAGEMENT,
            ModuleCategory.FINANCIAL_MANAGEMENT,
            ModuleCategory.ANALYTICS_REPORTING
        )
    ),

    HEALTHCARE(
        "Healthcare",
        "Medical practices, clinics, and healthcare providers",
        listOf(
            ModuleCategory.CUSTOMER_MANAGEMENT,
            ModuleCategory.COMMUNICATION,
            ModuleCategory.FINANCIAL_MANAGEMENT,
            ModuleCategory.ANALYTICS_REPORTING,
            ModuleCategory.HR_MANAGEMENT,
            ModuleCategory.ADMINISTRATION
        )
    ),

    EDUCATION(
        "Education",
        "Schools, training centers, and educational institutions",
        listOf(
            ModuleCategory.CUSTOMER_MANAGEMENT,
            ModuleCategory.COMMUNICATION,
            ModuleCategory.PROJECT_MANAGEMENT,
            ModuleCategory.FINANCIAL_MANAGEMENT,
            ModuleCategory.HR_MANAGEMENT,
            ModuleCategory.ANALYTICS_REPORTING
        )
    ),

    RESTAURANT(
        "Restaurant & Food Service",
        "Restaurants, cafes, and food service businesses",
        listOf(
            ModuleCategory.CUSTOMER_MANAGEMENT,
            ModuleCategory.INVENTORY_MANAGEMENT,
            ModuleCategory.ORDER_MANAGEMENT,
            ModuleCategory.FINANCIAL_MANAGEMENT,
            ModuleCategory.HR_MANAGEMENT,
            ModuleCategory.ANALYTICS_REPORTING
        )
    ),

    NONPROFIT(
        "Non-Profit",
        "Non-profit organizations and NGOs",
        listOf(
            ModuleCategory.CUSTOMER_MANAGEMENT,
            ModuleCategory.COMMUNICATION,
            ModuleCategory.PROJECT_MANAGEMENT,
            ModuleCategory.FINANCIAL_MANAGEMENT,
            ModuleCategory.HR_MANAGEMENT,
            ModuleCategory.ANALYTICS_REPORTING
        )
    ),

    TECHNOLOGY(
        "Technology",
        "Software companies, SaaS, and tech startups",
        listOf(
            ModuleCategory.CUSTOMER_MANAGEMENT,
            ModuleCategory.SALES_MANAGEMENT,
            ModuleCategory.PROJECT_MANAGEMENT,
            ModuleCategory.FINANCIAL_MANAGEMENT,
            ModuleCategory.COMMUNICATION,
            ModuleCategory.ANALYTICS_REPORTING,
            ModuleCategory.INTEGRATIONS,
            ModuleCategory.HR_MANAGEMENT
        )
    ),

    GENERAL(
        "General Business",
        "Other business types or mixed operations",
        ModuleCategory.values().toList()
    )
}

/**
 * Module complexity levels
 */
enum class ModuleComplexity(val displayName: String, val description: String) {
    ESSENTIAL("Essential", "Core business functions every business needs"),
    STANDARD("Standard", "Common business functions for growing businesses"),
    ADVANCED("Advanced", "Sophisticated features for established businesses"),
    SPECIALIZED("Specialized", "Industry-specific or niche functionality")
}