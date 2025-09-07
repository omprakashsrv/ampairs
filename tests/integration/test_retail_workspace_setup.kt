package com.ampairs.tests.integration

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles

/**
 * Integration tests for complete retail workspace setup workflow.
 * 
 * Tests the end-to-end process of creating and configuring a retail workspace
 * with all necessary modules, tax codes, and initial setup data.
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RetailWorkspaceSetupIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    private var createdWorkspaceId: String = ""
    private var authToken: String = "valid_jwt_token"

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @Order(1)
    @DisplayName("Step 1: Create KIRANA workspace with retail business type")
    fun `should create kirana workspace with retail configuration`() {
        val kiranaWorkspaceRequest = """
            {
                "name": "Sri Ganesh Kirana Store",
                "description": "Traditional neighborhood grocery store serving local community",
                "business_type": "KIRANA",
                "owner_details": {
                    "name": "Rajesh Kumar",
                    "phone": "+919876543210",
                    "email": "rajesh@sriganeshkirana.com",
                    "address": {
                        "street": "123 Main Bazaar Road",
                        "city": "Mysore",
                        "state": "Karnataka",
                        "postal_code": "570001",
                        "country": "India"
                    }
                },
                "business_details": {
                    "gstin": "29ABCDE1234F1Z5",
                    "pan": "ABCDE1234F",
                    "business_address": {
                        "street": "Shop No. 45, Gandhi Bazaar",
                        "city": "Mysore",
                        "state": "Karnataka",
                        "postal_code": "570001",
                        "country": "India"
                    }
                },
                "features": {
                    "local_inventory": true,
                    "credit_management": true,
                    "neighborhood_delivery": true,
                    "bulk_purchases": true
                }
            }
        """.trimIndent()

        val response = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .body(kiranaWorkspaceRequest)
            .`when`()
            .post("/workspace/v1")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("data.name", equalTo("Sri Ganesh Kirana Store"))
            .body("data.business_type", equalTo("KIRANA"))
            .body("data.status", equalTo("ACTIVE"))
            .body("data.features.local_inventory", equalTo(true))
            .body("data.features.credit_management", equalTo(true))
            .extract()

        createdWorkspaceId = response.path("data.id")
        println("Created workspace ID: $createdWorkspaceId")
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Install essential retail modules for KIRANA business")
    fun `should install essential retail modules for kirana workspace`() {
        val essentialModules = listOf(
            "product-management",
            "inventory-management", 
            "customer-management",
            "order-management",
            "invoice-generation",
            "tax-management"
        )

        essentialModules.forEach { moduleCode ->
            val moduleInstallRequest = """
                {
                    "module_code": "$moduleCode",
                    "auto_configure": true
                }
            """.trimIndent()

            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", createdWorkspaceId)
                .body(moduleInstallRequest)
                .`when`()
                .post("/workspace/v1/modules")
                .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.module_code", equalTo(moduleCode))
                .body("data.status", equalTo("ACTIVE"))
                .body("data.configuration", notNullValue())
        }
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Create essential tax codes for Indian GST compliance")
    fun `should create essential tax codes for GST compliance`() {
        val gstTaxCodes = listOf(
            // Essential food items - 5% GST
            """
            {
                "code": "GST-5-FOOD-ESSENTIALS",
                "name": "GST 5% - Essential Food Items",
                "description": "5% GST for rice, wheat, sugar, edible oil, etc.",
                "tax_type": "GST",
                "rate": 5.0,
                "hsn_code": "1006",
                "applicable_business_types": ["KIRANA"],
                "effective_from": "2025-09-07",
                "is_compound": true,
                "components": [
                    {"component_type": "CGST", "rate": 2.5, "account_code": "CGST_PAYABLE"},
                    {"component_type": "SGST", "rate": 2.5, "account_code": "SGST_PAYABLE"}
                ]
            }
            """,
            // Processed foods - 12% GST
            """
            {
                "code": "GST-12-PROCESSED-FOOD",
                "name": "GST 12% - Processed Food",
                "description": "12% GST for biscuits, namkeen, sweets, etc.",
                "tax_type": "GST",
                "rate": 12.0,
                "hsn_code": "1905",
                "applicable_business_types": ["KIRANA"],
                "effective_from": "2025-09-07",
                "is_compound": true,
                "components": [
                    {"component_type": "CGST", "rate": 6.0, "account_code": "CGST_PAYABLE"},
                    {"component_type": "SGST", "rate": 6.0, "account_code": "SGST_PAYABLE"}
                ]
            }
            """,
            // General goods - 18% GST
            """
            {
                "code": "GST-18-GENERAL",
                "name": "GST 18% - General Goods",
                "description": "18% GST for soaps, detergents, cosmetics, etc.",
                "tax_type": "GST",
                "rate": 18.0,
                "hsn_code": "3401",
                "applicable_business_types": ["KIRANA"],
                "effective_from": "2025-09-07",
                "is_compound": true,
                "components": [
                    {"component_type": "CGST", "rate": 9.0, "account_code": "CGST_PAYABLE"},
                    {"component_type": "SGST", "rate": 9.0, "account_code": "SGST_PAYABLE"}
                ]
            }
            """,
            // Exempt items - 0% GST
            """
            {
                "code": "GST-EXEMPT-BASIC",
                "name": "GST Exempt - Basic Necessities", 
                "description": "Tax exempt for milk, bread, vegetables",
                "tax_type": "GST",
                "rate": 0.0,
                "hsn_code": "0401",
                "applicable_business_types": ["KIRANA"],
                "effective_from": "2025-09-07",
                "is_exempt": true,
                "exemption_reason": "Essential commodities for public welfare"
            }
            """
        )

        gstTaxCodes.forEach { taxCodeJson ->
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", createdWorkspaceId)
                .body(taxCodeJson.trimIndent())
                .`when`()
                .post("/tax-code/v1")
                .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.status", equalTo("ACTIVE"))
        }
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Create sample product categories for kirana business")
    fun `should create product categories for organized inventory`() {
        val productCategories = listOf(
            """
            {
                "name": "Staples & Grains",
                "code": "STAPLES",
                "description": "Rice, wheat, pulses, flour",
                "tax_code": "GST-5-FOOD-ESSENTIALS",
                "attributes": ["weight", "brand", "quality_grade"]
            }
            """,
            """
            {
                "name": "Cooking Essentials",
                "code": "COOKING",
                "description": "Oil, spices, salt, sugar",
                "tax_code": "GST-5-FOOD-ESSENTIALS",
                "attributes": ["volume", "brand", "expiry_date"]
            }
            """,
            """
            {
                "name": "Packaged Foods",
                "code": "PACKAGED",
                "description": "Biscuits, snacks, instant foods",
                "tax_code": "GST-12-PROCESSED-FOOD",
                "attributes": ["pack_size", "brand", "expiry_date", "mrp"]
            }
            """,
            """
            {
                "name": "Personal Care",
                "code": "PERSONAL_CARE",
                "description": "Soaps, shampoos, toothpaste",
                "tax_code": "GST-18-GENERAL",
                "attributes": ["brand", "variant", "pack_size"]
            }
            """
        )

        productCategories.forEach { categoryJson ->
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", createdWorkspaceId)
                .body(categoryJson.trimIndent())
                .`when`()
                .post("/category/v1")
                .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.status", equalTo("ACTIVE"))
        }
    }

    @Test
    @Order(5)
    @DisplayName("Step 5: Create sample products for each category")
    fun `should create sample products for immediate business use`() {
        val sampleProducts = listOf(
            // Staples
            """
            {
                "name": "Basmati Rice Premium",
                "sku": "RICE-BAS-PREM-5KG",
                "description": "Premium quality basmati rice 5kg pack",
                "category_code": "STAPLES",
                "unit_id": "unit-kilograms",
                "tax_code": "GST-5-FOOD-ESSENTIALS",
                "base_price": 450.00,
                "cost_price": 380.00,
                "attributes": {
                    "weight_kg": 5.0,
                    "brand": "India Gate",
                    "quality_grade": "Premium"
                }
            }
            """,
            """
            {
                "name": "Toor Dal",
                "sku": "DAL-TOOR-1KG",
                "description": "Fresh toor dal 1kg pack",
                "category_code": "STAPLES", 
                "unit_id": "unit-kilograms",
                "tax_code": "GST-5-FOOD-ESSENTIALS",
                "base_price": 120.00,
                "cost_price": 100.00,
                "attributes": {
                    "weight_kg": 1.0,
                    "quality_grade": "Premium"
                }
            }
            """,
            // Cooking essentials
            """
            {
                "name": "Sunflower Oil",
                "sku": "OIL-SUNFLOWER-1L",
                "description": "Refined sunflower oil 1 liter",
                "category_code": "COOKING",
                "unit_id": "unit-liters",
                "tax_code": "GST-5-FOOD-ESSENTIALS",
                "base_price": 140.00,
                "cost_price": 125.00,
                "attributes": {
                    "volume_liters": 1.0,
                    "brand": "Fortune"
                }
            }
            """,
            // Packaged foods
            """
            {
                "name": "Parle-G Biscuits",
                "sku": "BISCUIT-PARLE-G-800G",
                "description": "Parle-G glucose biscuits family pack",
                "category_code": "PACKAGED",
                "unit_id": "unit-pieces",
                "tax_code": "GST-12-PROCESSED-FOOD",
                "base_price": 80.00,
                "cost_price": 70.00,
                "attributes": {
                    "pack_size": "800g",
                    "brand": "Parle"
                }
            }
            """,
            // Personal care
            """
            {
                "name": "Lux Soap",
                "sku": "SOAP-LUX-100G",
                "description": "Lux beauty soap 100g",
                "category_code": "PERSONAL_CARE",
                "unit_id": "unit-pieces",
                "tax_code": "GST-18-GENERAL",
                "base_price": 25.00,
                "cost_price": 20.00,
                "attributes": {
                    "brand": "Lux",
                    "variant": "Rose",
                    "pack_size": "100g"
                }
            }
            """
        )

        sampleProducts.forEach { productJson ->
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", createdWorkspaceId)
                .body(productJson.trimIndent())
                .`when`()
                .post("/product/v1")
                .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.status", equalTo("ACTIVE"))
        }
    }

    @Test
    @Order(6)
    @DisplayName("Step 6: Set initial inventory levels for sample products")
    fun `should set initial inventory for all sample products`() {
        val productSkus = listOf(
            "RICE-BAS-PREM-5KG",
            "DAL-TOOR-1KG", 
            "OIL-SUNFLOWER-1L",
            "BISCUIT-PARLE-G-800G",
            "SOAP-LUX-100G"
        )

        productSkus.forEach { sku ->
            val inventoryRequest = """
                {
                    "adjustment_type": "SET",
                    "quantity": 50.0,
                    "reason": "Initial stock setup for new kirana store",
                    "reorder_level": 10.0,
                    "max_stock_level": 200.0
                }
            """.trimIndent()

            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", createdWorkspaceId)
                .body(inventoryRequest)
                .`when`()
                .put("/product/v1/$sku/inventory")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.current_stock", equalTo(50.0f))
                .body("data.reorder_level", equalTo(10.0f))
        }
    }

    @Test
    @Order(7)
    @DisplayName("Step 7: Create sample customers for the kirana store")
    fun `should create sample customers representing local community`() {
        val sampleCustomers = listOf(
            // Regular retail customer
            """
            {
                "name": "Priya Sharma",
                "phone": "+919876543211",
                "customer_type": "RETAIL",
                "address": {
                    "street": "Apartment 2B, Vijayanagar",
                    "city": "Mysore",
                    "state": "Karnataka",
                    "postal_code": "570017",
                    "country": "India"
                },
                "attributes": {
                    "preferred_delivery_time": "EVENING",
                    "credit_eligible": true,
                    "loyalty_member": true
                }
            }
            """,
            // Small business wholesale customer
            """
            {
                "name": "Annapurna Tea Stall",
                "phone": "+919876543212",
                "customer_type": "WHOLESALE",
                "business_name": "Annapurna Tea Stall",
                "credit_limit": 5000.00,
                "credit_days": 15,
                "address": {
                    "street": "Market Complex, K.R. Circle",
                    "city": "Mysore",
                    "state": "Karnataka",
                    "postal_code": "570001",
                    "country": "India"
                },
                "attributes": {
                    "bulk_order_discount": 3.0,
                    "payment_terms": "NET_15",
                    "preferred_delivery_time": "MORNING"
                }
            }
            """
        )

        sampleCustomers.forEach { customerJson ->
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", createdWorkspaceId)
                .body(customerJson.trimIndent())
                .`when`()
                .post("/customer/v1")
                .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.status", equalTo("ACTIVE"))
        }
    }

    @Test
    @Order(8)
    @DisplayName("Step 8: Create a sample order to test end-to-end workflow")
    fun `should create and process a complete order workflow`() {
        // Step 8a: Create order
        val orderRequest = """
            {
                "customer_phone": "+919876543211",
                "line_items": [
                    {
                        "product_sku": "RICE-BAS-PREM-5KG",
                        "quantity": 2,
                        "unit_price": 450.00
                    },
                    {
                        "product_sku": "DAL-TOOR-1KG",
                        "quantity": 3,
                        "unit_price": 120.00
                    },
                    {
                        "product_sku": "SOAP-LUX-100G",
                        "quantity": 5,
                        "unit_price": 25.00
                    }
                ],
                "notes": "Regular monthly grocery order"
            }
        """.trimIndent()

        val createOrderResponse = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", createdWorkspaceId)
            .body(orderRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.status", equalTo("DRAFT"))
            .body("data.line_items", hasSize(3))
            .body("data.subtotal", equalTo(1485.0f)) // (2*450) + (3*120) + (5*25) = 900 + 360 + 125
            .body("data.tax_amount", greaterThan(0.0f))
            .body("data.total_amount", greaterThan(1485.0f))
            .extract()

        val orderId = createOrderResponse.path<String>("data.id")

        // Step 8b: Confirm order
        val confirmOrderRequest = """
            {
                "new_status": "CONFIRMED",
                "notes": "Customer confirmed order over phone"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", createdWorkspaceId)
            .body(confirmOrderRequest)
            .`when`()
            .put("/order/v1/$orderId/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("CONFIRMED"))
            .body("data.inventory_reserved", equalTo(true))

        // Step 8c: Mark order as ready
        val readyOrderRequest = """
            {
                "new_status": "READY",
                "notes": "All items packed and ready for pickup"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", createdWorkspaceId)
            .body(readyOrderRequest)
            .`when`()
            .put("/order/v1/$orderId/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("READY"))

        // Step 8d: Complete order and generate invoice
        val completeOrderRequest = """
            {
                "new_status": "COMPLETED",
                "notes": "Customer picked up and paid cash",
                "payment_method": "CASH",
                "payment_amount": 1560.75
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", createdWorkspaceId)
            .body(completeOrderRequest)
            .`when`()
            .put("/order/v1/$orderId/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("COMPLETED"))
            .body("data.invoice_generated", equalTo(true))
    }

    @Test
    @Order(9)
    @DisplayName("Step 9: Verify workspace analytics and reporting setup")
    fun `should have analytics and reporting capabilities set up`() {
        // Check workspace dashboard data
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", createdWorkspaceId)
            .`when`()
            .get("/workspace/v1/dashboard")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.product_count", greaterThan(0))
            .body("data.customer_count", greaterThan(0))
            .body("data.order_count", greaterThan(0))
            .body("data.revenue_today", greaterThanOrEqualTo(0.0f))

        // Check inventory levels
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", createdWorkspaceId)
            .queryParam("low_stock", true)
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))

        // Check tax summary
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", createdWorkspaceId)
            .queryParam("from_date", "2025-09-01")
            .queryParam("to_date", "2025-09-30")
            .queryParam("include_summary", true)
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.summary", notNullValue())
    }

    @Test
    @Order(10)
    @DisplayName("Step 10: Validate complete workspace configuration")
    fun `should have complete functional retail workspace`() {
        // Final workspace validation
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", createdWorkspaceId)
            .`when`()
            .get("/workspace/v1/current")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.business_type", equalTo("KIRANA"))
            .body("data.status", equalTo("ACTIVE"))
            .body("data.setup_complete", equalTo(true))
            .body("data.modules_installed", hasSize(greaterThan(5)))
            .body("data.tax_codes_configured", equalTo(true))
            .body("data.products_available", equalTo(true))
            .body("data.customers_registered", equalTo(true))
            .body("data.first_order_processed", equalTo(true))

        // Validate multi-tenant isolation
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", "DIFFERENT_WORKSPACE_ID")
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(0)) // Should not see products from other workspace
    }
}