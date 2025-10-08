package com.ampairs.workspace.model.enums

/**
 * Represents different types of business workspaces in the Ampairs system.
 * Each type defines the business model and operational structure.
 */
enum class WorkspaceType(
    val displayName: String,
    val description: String,
    val maxUsers: Int = 50,
    val features: List<String> = emptyList()
) {
    /**
     * Small business or startup
     */
    BUSINESS(
        "Business",
        "Small to medium business with basic needs",
        50,
        listOf("basic_inventory", "customer_management", "order_processing", "invoice_generation")
    ),

    /**
     * E-commerce business
     */
    ECOMMERCE(
        "E-commerce",
        "Online retail business with advanced e-commerce features",
        100,
        listOf("online_catalog", "payment_integration", "shipping_management", "marketplace_sync")
    ),

    /**
     * Retail store
     */
    RETAIL(
        "Retail",
        "Physical retail store with POS integration",
        25,
        listOf("pos_integration", "in_store_inventory", "loyalty_programs", "staff_management")
    ),

    /**
     * Wholesale distributor
     */
    WHOLESALE(
        "Wholesale",
        "Wholesale distribution business with bulk operations",
        200,
        listOf("bulk_pricing", "supplier_management", "distribution_tracking", "multi_location")
    ),

    /**
     * Manufacturing business
     */
    MANUFACTURING(
        "Manufacturing",
        "Manufacturing business with production tracking",
        500,
        listOf("production_planning", "raw_materials", "quality_control", "supply_chain")
    ),

    /**
     * Service-based business
     */
    SERVICE(
        "Service",
        "Service-based business with project and time tracking",
        30,
        listOf("time_tracking", "project_management", "service_billing", "client_portal")
    ),

    /**
     * Enterprise organization
     */
    ENTERPRISE(
        "Enterprise",
        "Large enterprise with advanced features and integrations",
        Int.MAX_VALUE,
        listOf("advanced_analytics", "custom_integrations", "multi_tenant", "enterprise_security")
    ),

    /**
     * Franchise business
     */
    FRANCHISE(
        "Franchise",
        "Franchise business with multi-location management",
        100,
        listOf("multi_location", "franchise_reporting", "centralized_inventory", "brand_compliance")
    ),

    /**
     * Kirana store - Indian neighborhood grocery store
     */
    KIRANA(
        "Kirana Store",
        "Traditional Indian neighborhood store with local customer base",
        15,
        listOf("local_inventory", "credit_management", "neighborhood_delivery", "bulk_purchases")
    ),

    /**
     * Jewelry store with precious metals and stones
     */
    JEWELRY(
        "Jewelry Store",
        "Jewelry retail with precious metals, gems, and custom designs",
        20,
        listOf("precious_metals", "custom_designs", "certification_tracking", "weight_based_pricing")
    ),

    /**
     * Hardware and construction supplies store
     */
    HARDWARE(
        "Hardware Store",
        "Hardware and construction supplies with bulk inventory",
        30,
        listOf("bulk_inventory", "construction_supplies", "contractor_accounts", "delivery_logistics")
    )
}