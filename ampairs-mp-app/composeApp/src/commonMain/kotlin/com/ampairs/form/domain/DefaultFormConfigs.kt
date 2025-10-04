package com.ampairs.form.domain

/**
 * Default form configurations for entities
 *
 * ⚠️ FOR BACKEND REFERENCE ONLY ⚠️
 *
 * This file serves as documentation and reference for backend developers
 * to seed default form configurations in the database.
 *
 * The backend should automatically seed these configurations when:
 * - GET /api/v1/form/schema?entity_type=customer is called
 * - No configuration exists for that entity type
 * - Return the seeded configuration to the client
 *
 * This prevents duplicate configurations when multiple users access
 * the system for the first time.
 */
object DefaultFormConfigs {

    /**
     * Default customer form field configurations
     * Matches the static fields in CustomerFormScreen
     */
    fun getDefaultCustomerFieldConfigs(): List<EntityFieldConfig> = listOf(
        // === Basic Information Section ===
        EntityFieldConfig(
            uid = "customer-field-name",
            entityType = "customer",
            fieldName = "name",
            displayName = "Customer Name",
            visible = true,
            mandatory = true,
            enabled = true,
            displayOrder = 1,
            placeholder = "Enter customer name",
            helpText = "Full name of the customer"
        ),
        EntityFieldConfig(
            uid = "customer-field-email",
            entityType = "customer",
            fieldName = "email",
            displayName = "Email",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 2,
            validationType = "email",
            placeholder = "customer@example.com"
        ),
        EntityFieldConfig(
            uid = "customer-field-customerType",
            entityType = "customer",
            fieldName = "customerType",
            displayName = "Customer Type",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 3,
            validationType = "select",
            helpText = "Type of customer (Retail, Wholesale, etc.)"
        ),
        EntityFieldConfig(
            uid = "customer-field-customerGroup",
            entityType = "customer",
            fieldName = "customerGroup",
            displayName = "Customer Group",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 4,
            validationType = "select",
            helpText = "Group classification for customer"
        ),
        EntityFieldConfig(
            uid = "customer-field-phone",
            entityType = "customer",
            fieldName = "phone",
            displayName = "Phone Number",
            visible = true,
            mandatory = true,
            enabled = true,
            displayOrder = 5,
            validationType = "phone",
            placeholder = "Enter phone number"
        ),
        EntityFieldConfig(
            uid = "customer-field-landline",
            entityType = "customer",
            fieldName = "landline",
            displayName = "Landline",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 6,
            placeholder = "Enter landline number"
        ),

        // === Business Information Section ===
        EntityFieldConfig(
            uid = "customer-field-gstNumber",
            entityType = "customer",
            fieldName = "gstNumber",
            displayName = "GST Number",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 7,
            validationType = "gstin",
            placeholder = "Enter 15-digit GST number"
        ),
        EntityFieldConfig(
            uid = "customer-field-panNumber",
            entityType = "customer",
            fieldName = "panNumber",
            displayName = "PAN Number",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 8,
            validationType = "pan",
            placeholder = "Enter 10-digit PAN"
        ),

        // === Credit Management Section ===
        EntityFieldConfig(
            uid = "customer-field-creditLimit",
            entityType = "customer",
            fieldName = "creditLimit",
            displayName = "Credit Limit",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 9,
            validationType = "number",
            placeholder = "0.00",
            helpText = "Maximum credit amount allowed"
        ),
        EntityFieldConfig(
            uid = "customer-field-creditDays",
            entityType = "customer",
            fieldName = "creditDays",
            displayName = "Credit Days",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 10,
            validationType = "number",
            placeholder = "0",
            helpText = "Number of days for credit payment"
        ),

        // === Main Address Section ===
        EntityFieldConfig(
            uid = "customer-field-address",
            entityType = "customer",
            fieldName = "address",
            displayName = "Address",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 11,
            placeholder = "Enter address"
        ),
        EntityFieldConfig(
            uid = "customer-field-street",
            entityType = "customer",
            fieldName = "street",
            displayName = "Street",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 12,
            placeholder = "Enter street name"
        ),
        EntityFieldConfig(
            uid = "customer-field-street2",
            entityType = "customer",
            fieldName = "street2",
            displayName = "Street 2",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 13,
            placeholder = "Additional street information"
        ),
        EntityFieldConfig(
            uid = "customer-field-city",
            entityType = "customer",
            fieldName = "city",
            displayName = "City",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 14,
            placeholder = "Enter city"
        ),
        EntityFieldConfig(
            uid = "customer-field-pincode",
            entityType = "customer",
            fieldName = "pincode",
            displayName = "PIN Code",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 15,
            placeholder = "Enter PIN code"
        ),
        EntityFieldConfig(
            uid = "customer-field-state",
            entityType = "customer",
            fieldName = "state",
            displayName = "State",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 16,
            placeholder = "Select state"
        ),
        EntityFieldConfig(
            uid = "customer-field-country",
            entityType = "customer",
            fieldName = "country",
            displayName = "Country",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 17,
            placeholder = "India",
            defaultValue = "India"
        ),

        // === Location Section ===
        EntityFieldConfig(
            uid = "customer-field-latitude",
            entityType = "customer",
            fieldName = "latitude",
            displayName = "Latitude",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 18,
            validationType = "number",
            helpText = "GPS latitude coordinate"
        ),
        EntityFieldConfig(
            uid = "customer-field-longitude",
            entityType = "customer",
            fieldName = "longitude",
            displayName = "Longitude",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 19,
            validationType = "number",
            helpText = "GPS longitude coordinate"
        ),

        // === Billing Address Section ===
        EntityFieldConfig(
            uid = "customer-field-billingStreet",
            entityType = "customer",
            fieldName = "billingStreet",
            displayName = "Billing Street",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 20,
            placeholder = "Enter billing street"
        ),
        EntityFieldConfig(
            uid = "customer-field-billingCity",
            entityType = "customer",
            fieldName = "billingCity",
            displayName = "Billing City",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 21,
            placeholder = "Enter billing city"
        ),
        EntityFieldConfig(
            uid = "customer-field-billingPincode",
            entityType = "customer",
            fieldName = "billingPincode",
            displayName = "Billing PIN Code",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 22,
            placeholder = "Enter billing PIN"
        ),
        EntityFieldConfig(
            uid = "customer-field-billingState",
            entityType = "customer",
            fieldName = "billingState",
            displayName = "Billing State",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 23,
            placeholder = "Select billing state"
        ),
        EntityFieldConfig(
            uid = "customer-field-billingCountry",
            entityType = "customer",
            fieldName = "billingCountry",
            displayName = "Billing Country",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 24,
            placeholder = "India",
            defaultValue = "India"
        ),

        // === Shipping Address Section ===
        EntityFieldConfig(
            uid = "customer-field-shippingStreet",
            entityType = "customer",
            fieldName = "shippingStreet",
            displayName = "Shipping Street",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 25,
            placeholder = "Enter shipping street"
        ),
        EntityFieldConfig(
            uid = "customer-field-shippingCity",
            entityType = "customer",
            fieldName = "shippingCity",
            displayName = "Shipping City",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 26,
            placeholder = "Enter shipping city"
        ),
        EntityFieldConfig(
            uid = "customer-field-shippingPincode",
            entityType = "customer",
            fieldName = "shippingPincode",
            displayName = "Shipping PIN Code",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 27,
            placeholder = "Enter shipping PIN"
        ),
        EntityFieldConfig(
            uid = "customer-field-shippingState",
            entityType = "customer",
            fieldName = "shippingState",
            displayName = "Shipping State",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 28,
            placeholder = "Select shipping state"
        ),
        EntityFieldConfig(
            uid = "customer-field-shippingCountry",
            entityType = "customer",
            fieldName = "shippingCountry",
            displayName = "Shipping Country",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 29,
            placeholder = "India",
            defaultValue = "India"
        ),

        // === Status Section ===
        EntityFieldConfig(
            uid = "customer-field-status",
            entityType = "customer",
            fieldName = "status",
            displayName = "Status",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = 30,
            validationType = "select",
            defaultValue = "ACTIVE",
            helpText = "Customer status (Active, Inactive, Suspended)"
        )
    )

    /**
     * Default customer attribute definitions
     * These are custom fields that can be added beyond standard fields
     */
    fun getDefaultCustomerAttributeDefinitions(): List<EntityAttributeDefinition> = emptyList()

    /**
     * Get default config schema for any entity type
     */
    fun getDefaultConfigSchema(entityType: String): EntityConfigSchema {
        return when (entityType.lowercase()) {
            "customer" -> EntityConfigSchema(
                fieldConfigs = getDefaultCustomerFieldConfigs(),
                attributeDefinitions = getDefaultCustomerAttributeDefinitions()
            )
            // Add other entity types as needed
            else -> EntityConfigSchema(
                fieldConfigs = emptyList(),
                attributeDefinitions = emptyList()
            )
        }
    }
}
