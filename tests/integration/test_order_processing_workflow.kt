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
 * Integration tests for complete order processing workflow.
 * 
 * Tests the end-to-end process of order creation, status transitions,
 * inventory management, and multi-channel order handling across different retail types.
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class OrderProcessingWorkflowIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    private var kiranaWorkspaceId: String = ""
    private var jewelryWorkspaceId: String = ""
    private var authToken: String = "valid_jwt_token"
    private var createdOrderIds: MutableList<String> = mutableListOf()

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @Order(1)
    @DisplayName("Setup: Create test workspaces with products and customers")
    fun `should create test environments for order processing`() {
        // Create Kirana workspace
        val kiranaWorkspaceRequest = """
            {
                "name": "Fresh Mart Kirana",
                "description": "Neighborhood grocery store with home delivery",
                "business_type": "KIRANA",
                "owner_details": {
                    "name": "Ramesh Gupta",
                    "phone": "+919876543230",
                    "email": "ramesh@freshmart.com"
                }
            }
        """.trimIndent()

        val kiranaResponse = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .body(kiranaWorkspaceRequest)
            .`when`()
            .post("/workspace/v1")
            .then()
            .statusCode(201)
            .extract()

        kiranaWorkspaceId = kiranaResponse.path("data.id")

        // Create Jewelry workspace
        val jewelryWorkspaceRequest = """
            {
                "name": "Royal Gems Palace",
                "description": "Premium jewelry showroom with custom orders",
                "business_type": "JEWELRY",
                "owner_details": {
                    "name": "Kavitha Reddy",
                    "phone": "+919876543231",
                    "email": "kavitha@royalgems.com"
                }
            }
        """.trimIndent()

        val jewelryResponse = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .body(jewelryWorkspaceRequest)
            .`when`()
            .post("/workspace/v1")
            .then()
            .statusCode(201)
            .extract()

        jewelryWorkspaceId = jewelryResponse.path("data.id")

        // Setup products for Kirana
        setupKiranaProducts()
        
        // Setup products for Jewelry
        setupJewelryProducts()
        
        // Setup customers for both businesses
        setupCustomers()
    }

    private fun setupKiranaProducts() {
        val kiranaProducts = listOf(
            """{"name": "Basmati Rice 5kg", "sku": "RICE-BAS-5KG", "unit_id": "unit-pieces", "tax_code_id": "tax-gst-5", "base_price": 650.00, "cost_price": 580.00}""",
            """{"name": "Fortune Sunflower Oil 1L", "sku": "OIL-FORTUNE-1L", "unit_id": "unit-pieces", "tax_code_id": "tax-gst-5", "base_price": 140.00, "cost_price": 125.00}""",
            """{"name": "Parle-G Biscuits 200g", "sku": "BISCUIT-PARLE-200G", "unit_id": "unit-pieces", "tax_code_id": "tax-gst-12", "base_price": 20.00, "cost_price": 17.00}""",
            """{"name": "Colgate Toothpaste", "sku": "TOOTHPASTE-COLGATE-100G", "unit_id": "unit-pieces", "tax_code_id": "tax-gst-18", "base_price": 85.00, "cost_price": 70.00}""",
            """{"name": "Maggi Noodles 4-pack", "sku": "NOODLES-MAGGI-4PACK", "unit_id": "unit-pieces", "tax_code_id": "tax-gst-12", "base_price": 56.00, "cost_price": 48.00}"""
        )

        kiranaProducts.forEach { productJson ->
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", kiranaWorkspaceId)
                .body(productJson)
                .`when`()
                .post("/product/v1")
                .then()
                .statusCode(201)

            // Set inventory for each product
            val sku = when {
                productJson.contains("RICE-BAS-5KG") -> "RICE-BAS-5KG"
                productJson.contains("OIL-FORTUNE-1L") -> "OIL-FORTUNE-1L"
                productJson.contains("BISCUIT-PARLE-200G") -> "BISCUIT-PARLE-200G"
                productJson.contains("TOOTHPASTE-COLGATE-100G") -> "TOOTHPASTE-COLGATE-100G"
                else -> "NOODLES-MAGGI-4PACK"
            }

            val inventoryRequest = """{"adjustment_type": "SET", "quantity": 100.0, "reason": "Initial stock"}"""
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", kiranaWorkspaceId)
                .body(inventoryRequest)
                .`when`()
                .put("/product/v1/$sku/inventory")
                .then()
                .statusCode(200)
        }
    }

    private fun setupJewelryProducts() {
        val jewelryProducts = listOf(
            """{"name": "Gold Chain 22K", "sku": "GOLD-CHAIN-22K-10G", "unit_id": "unit-pieces", "tax_code_id": "tax-gst-3", "base_price": 55000.00, "cost_price": 52000.00, "attributes": {"weight_grams": "10.0", "purity": "22K"}}""",
            """{"name": "Diamond Earrings", "sku": "DIAMOND-EARRINGS-05CT", "unit_id": "unit-pieces", "tax_code_id": "tax-gst-3", "base_price": 85000.00, "cost_price": 78000.00, "attributes": {"diamond_carats": "0.5", "diamond_clarity": "VS1"}}""",
            """{"name": "Silver Bracelet", "sku": "SILVER-BRACELET-925", "unit_id": "unit-pieces", "tax_code_id": "tax-gst-12", "base_price": 2500.00, "cost_price": 2000.00, "attributes": {"weight_grams": "15.0", "purity": "925"}}"""
        )

        jewelryProducts.forEach { productJson ->
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", jewelryWorkspaceId)
                .body(productJson)
                .`when`()
                .post("/product/v1")
                .then()
                .statusCode(201)

            val sku = when {
                productJson.contains("GOLD-CHAIN-22K-10G") -> "GOLD-CHAIN-22K-10G"
                productJson.contains("DIAMOND-EARRINGS-05CT") -> "DIAMOND-EARRINGS-05CT"
                else -> "SILVER-BRACELET-925"
            }

            val inventoryRequest = """{"adjustment_type": "SET", "quantity": 5.0, "reason": "Initial stock"}"""
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", jewelryWorkspaceId)
                .body(inventoryRequest)
                .`when`()
                .put("/product/v1/$sku/inventory")
                .then()
                .statusCode(200)
        }
    }

    private fun setupCustomers() {
        // Create customers for Kirana
        val kiranaCustomers = listOf(
            """{"name": "Priya Sharma", "phone": "+919876543240", "customer_type": "RETAIL", "address": {"street": "Apt 15, Vijayanagar", "city": "Bangalore", "state": "Karnataka", "postal_code": "560040", "country": "India"}}""",
            """{"name": "Anand Tea Stall", "phone": "+919876543241", "customer_type": "WHOLESALE", "business_name": "Anand Tea Stall", "credit_limit": 5000.00, "credit_days": 15, "address": {"street": "Market Road", "city": "Bangalore", "state": "Karnataka", "postal_code": "560001", "country": "India"}}"""
        )

        kiranaCustomers.forEach { customerJson ->
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", kiranaWorkspaceId)
                .body(customerJson)
                .`when`()
                .post("/customer/v1")
                .then()
                .statusCode(201)
        }

        // Create customers for Jewelry
        val jewelryCustomers = listOf(
            """{"name": "Deepika Rao", "phone": "+919876543250", "customer_type": "RETAIL", "address": {"street": "Villa 25, Koramangala", "city": "Bangalore", "state": "Karnataka", "postal_code": "560095", "country": "India"}}""",
            """{"name": "Wedding Planners Inc", "phone": "+919876543251", "customer_type": "WHOLESALE", "business_name": "Royal Wedding Planners", "credit_limit": 500000.00, "credit_days": 30, "address": {"street": "Commercial Complex", "city": "Bangalore", "state": "Karnataka", "postal_code": "560001", "country": "India"}}"""
        )

        jewelryCustomers.forEach { customerJson ->
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", jewelryWorkspaceId)
                .body(customerJson)
                .`when`()
                .post("/customer/v1")
                .then()
                .statusCode(201)
        }
    }

    @Test
    @Order(2)
    @DisplayName("Step 1: Create regular retail order for kirana store")
    fun `should create retail order with multiple line items and proper tax calculations`() {
        val kiranaOrderRequest = """
            {
                "customer_phone": "+919876543240",
                "line_items": [
                    {
                        "product_sku": "RICE-BAS-5KG",
                        "quantity": 2,
                        "unit_price": 650.00
                    },
                    {
                        "product_sku": "OIL-FORTUNE-1L",
                        "quantity": 3,
                        "unit_price": 140.00
                    },
                    {
                        "product_sku": "BISCUIT-PARLE-200G",
                        "quantity": 10,
                        "unit_price": 20.00
                    },
                    {
                        "product_sku": "TOOTHPASTE-COLGATE-100G",
                        "quantity": 2,
                        "unit_price": 85.00
                    }
                ],
                "notes": "Regular grocery order - deliver evening",
                "delivery_address": {
                    "street": "Apt 15, Vijayanagar",
                    "city": "Bangalore",
                    "delivery_instructions": "Ring bell, apartment on 2nd floor"
                }
            }
        """.trimIndent()

        val response = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .body(kiranaOrderRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.status", equalTo("DRAFT"))
            .body("data.line_items", hasSize(4))
            .body("data.subtotal", equalTo(1890.0f)) // (2*650) + (3*140) + (10*20) + (2*85) = 1300+420+200+170
            .body("data.tax_amount", greaterThan(0.0f))
            .body("data.total_amount", greaterThan(1890.0f))
            .body("data.customer.name", equalTo("Priya Sharma"))
            .body("data.order_number", matchesRegex("ORD-\\d{8}-\\d{3}"))
            .extract()

        val orderId = response.path<String>("data.id")
        createdOrderIds.add(orderId)
        println("Created Kirana retail order: $orderId")
    }

    @Test
    @Order(3)
    @DisplayName("Step 2: Create wholesale order with credit terms")
    fun `should create wholesale order with credit limit validation`() {
        val wholesaleOrderRequest = """
            {
                "customer_phone": "+919876543241",
                "line_items": [
                    {
                        "product_sku": "RICE-BAS-5KG",
                        "quantity": 10,
                        "unit_price": 650.00
                    },
                    {
                        "product_sku": "NOODLES-MAGGI-4PACK",
                        "quantity": 20,
                        "unit_price": 56.00
                    }
                ],
                "order_type": "WHOLESALE",
                "payment_terms": "CREDIT",
                "credit_days": 15,
                "discount_percentage": 5.0,
                "notes": "Wholesale order for tea stall - regular supplier discount"
            }
        """.trimIndent()

        val response = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .body(wholesaleOrderRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.status", equalTo("DRAFT"))
            .body("data.order_type", equalTo("WHOLESALE"))
            .body("data.payment_terms", equalTo("CREDIT"))
            .body("data.credit_days", equalTo(15))
            .body("data.subtotal", equalTo(7620.0f)) // (10*650) + (20*56) = 6500+1120
            .body("data.discount_percentage", equalTo(5.0f))
            .body("data.discount_amount", greaterThan(0.0f))
            .body("data.customer.business_name", equalTo("Anand Tea Stall"))
            .extract()

        val orderId = response.path<String>("data.id")
        createdOrderIds.add(orderId)
        println("Created Kirana wholesale order: $orderId")
    }

    @Test
    @Order(4)
    @DisplayName("Step 3: Create luxury jewelry order with custom requirements")
    fun `should create luxury jewelry order with custom specifications`() {
        val jewelryOrderRequest = """
            {
                "customer_phone": "+919876543250",
                "line_items": [
                    {
                        "product_sku": "GOLD-CHAIN-22K-10G",
                        "quantity": 1,
                        "unit_price": 55000.00,
                        "customization_notes": "Engrave 'D & R' on pendant"
                    },
                    {
                        "product_sku": "DIAMOND-EARRINGS-05CT",
                        "quantity": 1,
                        "unit_price": 85000.00,
                        "customization_notes": "Gift wrapping required"
                    }
                ],
                "order_type": "CUSTOM",
                "special_instructions": "Wedding gift - premium packaging required",
                "estimated_delivery_days": 7,
                "advance_payment_percentage": 50.0,
                "notes": "High-value jewelry order - handle with care"
            }
        """.trimIndent()

        val response = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .body(jewelryOrderRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.status", equalTo("DRAFT"))
            .body("data.order_type", equalTo("CUSTOM"))
            .body("data.subtotal", equalTo(140000.0f)) // 55000 + 85000
            .body("data.advance_payment_percentage", equalTo(50.0f))
            .body("data.advance_payment_amount", equalTo(70000.0f))
            .body("data.estimated_delivery_days", equalTo(7))
            .body("data.line_items[0].customization_notes", equalTo("Engrave 'D & R' on pendant"))
            .extract()

        val orderId = response.path<String>("data.id")
        createdOrderIds.add(orderId)
        println("Created Jewelry custom order: $orderId")
    }

    @Test
    @Order(5)
    @DisplayName("Step 4: Create bulk order for wedding planners")
    fun `should create bulk jewelry order with quantity discounts`() {
        val bulkOrderRequest = """
            {
                "customer_phone": "+919876543251",
                "line_items": [
                    {
                        "product_sku": "GOLD-CHAIN-22K-10G",
                        "quantity": 5,
                        "unit_price": 55000.00
                    },
                    {
                        "product_sku": "SILVER-BRACELET-925",
                        "quantity": 10,
                        "unit_price": 2500.00
                    }
                ],
                "order_type": "BULK",
                "payment_terms": "CREDIT",
                "credit_days": 30,
                "bulk_discount_percentage": 8.0,
                "notes": "Wedding order for 50-guest celebration"
            }
        """.trimIndent()

        val response = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .body(bulkOrderRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.status", equalTo("DRAFT"))
            .body("data.order_type", equalTo("BULK"))
            .body("data.subtotal", equalTo(300000.0f)) // (5*55000) + (10*2500) = 275000+25000
            .body("data.bulk_discount_percentage", equalTo(8.0f))
            .body("data.discount_amount", greaterThan(20000.0f)) // 8% of 300000
            .body("data.customer.business_name", equalTo("Royal Wedding Planners"))
            .extract()

        val orderId = response.path<String>("data.id")
        createdOrderIds.add(orderId)
        println("Created Jewelry bulk order: $orderId")
    }

    @Test
    @Order(6)
    @DisplayName("Step 5: Process order status transitions for retail order")
    fun `should process complete order lifecycle with inventory management`() {
        val retailOrderId = createdOrderIds[0] // First order created

        // Step 5a: Confirm order
        val confirmRequest = """
            {
                "new_status": "CONFIRMED",
                "notes": "Customer confirmed order via phone call",
                "estimated_delivery_time": "2025-09-08T18:00:00Z"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .body(confirmRequest)
            .`when`()
            .put("/order/v1/$retailOrderId/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("CONFIRMED"))
            .body("data.inventory_reserved", equalTo(true))
            .body("data.estimated_delivery_time", equalTo("2025-09-08T18:00:00Z"))

        // Step 5b: Start processing
        val processingRequest = """
            {
                "new_status": "PROCESSING",
                "notes": "Started packing items for delivery",
                "assigned_staff": "Ravi Kumar"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .body(processingRequest)
            .`when`()
            .put("/order/v1/$retailOrderId/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("PROCESSING"))
            .body("data.processing_started_at", notNullValue())
            .body("data.assigned_staff", equalTo("Ravi Kumar"))

        // Step 5c: Mark ready for delivery
        val readyRequest = """
            {
                "new_status": "READY",
                "notes": "All items packed and ready for delivery",
                "delivery_tracking_id": "DEL-20250907-001"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .body(readyRequest)
            .`when`()
            .put("/order/v1/$retailOrderId/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("READY"))
            .body("data.ready_at", notNullValue())
            .body("data.delivery_tracking_id", equalTo("DEL-20250907-001"))

        // Step 5d: Complete order with payment
        val completeRequest = """
            {
                "new_status": "COMPLETED",
                "notes": "Order delivered and payment received",
                "payment_method": "UPI",
                "payment_reference": "UPI123456789",
                "payment_amount": 2010.15
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .body(completeRequest)
            .`when`()
            .put("/order/v1/$retailOrderId/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("COMPLETED"))
            .body("data.completed_at", notNullValue())
            .body("data.payment_method", equalTo("UPI"))
            .body("data.payment_reference", equalTo("UPI123456789"))
            .body("data.invoice_generated", equalTo(true))

        // Verify inventory was properly deducted
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .`when`()
            .get("/product/v1/RICE-BAS-5KG/inventory")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.current_stock", equalTo(98.0f)) // 100 - 2 sold
    }

    @Test
    @Order(7)
    @DisplayName("Step 6: Handle order cancellation with inventory release")
    fun `should handle order cancellation and release reserved inventory`() {
        val wholesaleOrderId = createdOrderIds[1] // Second order created

        // First confirm the order to reserve inventory
        val confirmRequest = """
            {
                "new_status": "CONFIRMED",
                "notes": "Wholesale order confirmed"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .body(confirmRequest)
            .`when`()
            .put("/order/v1/$wholesaleOrderId/status")
            .then()
            .statusCode(200)
            .body("data.inventory_reserved", equalTo(true))

        // Then cancel the order
        val cancelRequest = """
            {
                "new_status": "CANCELLED",
                "cancellation_reason": "CUSTOMER_REQUEST",
                "notes": "Customer cancelled due to cash flow issues",
                "refund_required": false
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .body(cancelRequest)
            .`when`()
            .put("/order/v1/$wholesaleOrderId/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("CANCELLED"))
            .body("data.cancellation_reason", equalTo("CUSTOMER_REQUEST"))
            .body("data.cancelled_at", notNullValue())
            .body("data.inventory_released", equalTo(true))
            .body("data.refund_required", equalTo(false))

        // Verify inventory was released back
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .`when`()
            .get("/product/v1/RICE-BAS-5KG/inventory")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.available_stock", equalTo(98.0f)) // Should be back to available after cancellation
    }

    @Test
    @Order(8)
    @DisplayName("Step 7: Process jewelry custom order with advance payment")
    fun `should handle custom jewelry order with partial payment workflow`() {
        val customOrderId = createdOrderIds[2] // Third order created (jewelry custom)

        // Confirm custom order with advance payment
        val confirmCustomRequest = """
            {
                "new_status": "CONFIRMED",
                "notes": "Customer paid 50% advance for custom jewelry order",
                "advance_payment_received": true,
                "advance_payment_method": "CARD",
                "advance_payment_reference": "CARD987654321",
                "estimated_completion_date": "2025-09-14"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .body(confirmCustomRequest)
            .`when`()
            .put("/order/v1/$customOrderId/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("CONFIRMED"))
            .body("data.advance_payment_received", equalTo(true))
            .body("data.advance_payment_method", equalTo("CARD"))
            .body("data.estimated_completion_date", equalTo("2025-09-14"))

        // Start custom work processing
        val processingCustomRequest = """
            {
                "new_status": "PROCESSING",
                "notes": "Started custom engraving and packaging work",
                "craftsman_assigned": "Master Gopal",
                "work_start_date": "2025-09-08"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .body(processingCustomRequest)
            .`when`()
            .put("/order/v1/$customOrderId/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("PROCESSING"))
            .body("data.craftsman_assigned", equalTo("Master Gopal"))

        // Complete custom order
        val completeCustomRequest = """
            {
                "new_status": "COMPLETED",
                "notes": "Custom jewelry completed - customer picked up and paid balance",
                "payment_method": "CASH",
                "balance_payment_amount": 70000.00,
                "final_payment_reference": "CASH20250914-001"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .body(completeCustomRequest)
            .`when`()
            .put("/order/v1/$customOrderId/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("COMPLETED"))
            .body("data.balance_payment_amount", equalTo(70000.0f))
            .body("data.total_amount_paid", greaterThan(140000.0f)) // advance + balance + tax
    }

    @Test
    @Order(9)
    @DisplayName("Step 8: Test order search and filtering capabilities")
    fun `should support comprehensive order search and filtering`() {
        // Search by customer name
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .queryParam("search", "Priya")
            .`when`()
            .get("/order/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content.findAll { it.customer.name.contains('Priya') }", hasSize(greaterThan(0)))

        // Filter by order status
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("status", "COMPLETED")
            .`when`()
            .get("/order/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("status", "COMPLETED")))

        // Filter by order type
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("order_type", "CUSTOM")
            .`when`()
            .get("/order/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("order_type", "CUSTOM")))

        // Filter by date range
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .queryParam("from_date", "2025-09-01")
            .queryParam("to_date", "2025-09-30")
            .queryParam("include_summary", true)
            .`when`()
            .get("/order/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.summary.total_orders", greaterThan(0))
            .body("data.summary.total_revenue", greaterThan(0.0f))
    }

    @Test
    @Order(10)
    @DisplayName("Step 9: Test bulk order operations and management")
    fun `should support bulk order operations and status updates`() {
        // Get pending orders for bulk status update
        val pendingOrdersResponse = RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("status", "DRAFT")
            .`when`()
            .get("/order/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .extract()

        val pendingOrderIds = pendingOrdersResponse.path<List<String>>("data.content.id")

        if (pendingOrderIds.isNotEmpty()) {
            // Bulk confirm orders
            val bulkUpdateRequest = """
                {
                    "order_ids": ${pendingOrderIds.take(2).map { "\"$it\"" }},
                    "new_status": "CONFIRMED",
                    "notes": "Bulk confirmation of jewelry orders"
                }
            """.trimIndent()

            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", jewelryWorkspaceId)
                .body(bulkUpdateRequest)
                .`when`()
                .put("/order/v1/bulk/status")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.updated_count", greaterThan(0))
                .body("data.failed_updates", hasSize(0))
        }

        // Test order export functionality
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .queryParam("export_format", "EXCEL")
            .queryParam("from_date", "2025-09-01")
            .queryParam("to_date", "2025-09-30")
            .queryParam("include_customer_details", true)
            .`when`()
            .get("/order/v1/list/export")
            .then()
            .statusCode(200)
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    }

    @Test
    @Order(11)
    @DisplayName("Step 10: Validate complete order processing workflow")
    fun `should have complete order processing capabilities across business types`() {
        // Validate Kirana order statistics
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .queryParam("include_analytics", true)
            .`when`()
            .get("/order/v1/analytics")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.total_orders", greaterThan(0))
            .body("data.completed_orders", greaterThan(0))
            .body("data.cancelled_orders", greaterThan(0))
            .body("data.total_revenue", greaterThan(0.0f))
            .body("data.average_order_value", greaterThan(0.0f))

        // Validate Jewelry order statistics
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("include_analytics", true)
            .`when`()
            .get("/order/v1/analytics")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.total_orders", greaterThan(0))
            .body("data.high_value_orders", greaterThan(0)) // Jewelry orders are typically high-value
            .body("data.custom_orders", greaterThan(0))
            .body("data.total_revenue", greaterThan(100000.0f)) // Jewelry has higher revenue

        // Validate multi-tenant isolation
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .queryParam("search", "gold")
            .`when`()
            .get("/order/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(0)) // Kirana shouldn't see jewelry orders

        // Test order performance
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("performance_mode", true)
            .`when`()
            .get("/order/v1/list")
            .then()
            .statusCode(200)
            .time(lessThan(2000L)) // Should respond within 2 seconds
            .body("success", equalTo(true))
    }
}