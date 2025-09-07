package com.ampairs.tests.contract

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles

/**
 * Contract tests for Order Management API - Create Order endpoint.
 * 
 * Tests verify the POST /order/v1 endpoint according to the retail API contract.
 * Covers order creation with line items, inventory checking, and tax calculations.
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderCreateContractTest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @DisplayName("POST /order/v1 - Create basic order with line items")
    fun `should create order with multiple line items and calculate totals`() {
        val orderRequest = """
            {
                "customer_id": "CUST_HARDWARE_001",
                "line_items": [
                    {
                        "product_id": "PROD_HAMMER_001",
                        "quantity": 2,
                        "unit_price": 450.00
                    },
                    {
                        "product_id": "PROD_SCREWS_001", 
                        "quantity": 5,
                        "unit_price": 25.00,
                        "discount_amount": 5.00
                    }
                ],
                "discount_amount": 20.00,
                "notes": "Bulk order for construction site"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_HARDWARE_WS_001")
            .body(orderRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("data.id", notNullValue())
            .body("data.order_number", notNullValue())
            .body("data.status", equalTo("DRAFT"))
            .body("data.customer.id", equalTo("CUST_HARDWARE_001"))
            .body("data.line_items", hasSize(2))
            .body("data.line_items[0].product_id", equalTo("PROD_HAMMER_001"))
            .body("data.line_items[0].quantity", equalTo(2))
            .body("data.line_items[0].unit_price", equalTo(450.0f))
            .body("data.subtotal", equalTo(1020.0f)) // (2*450) + (5*25) = 900 + 125
            .body("data.tax_amount", greaterThan(0.0f))
            .body("data.discount_amount", equalTo(25.0f)) // 20 + 5 (line item discount)
            .body("data.total_amount", notNullValue())
            .body("data.created_at", notNullValue())
    }

    @Test
    @DisplayName("POST /order/v1 - Create order without customer (walk-in)")
    fun `should create walk-in order without customer reference`() {
        val walkInOrderRequest = """
            {
                "line_items": [
                    {
                        "product_id": "PROD_RICE_001",
                        "quantity": 2,
                        "unit_price": 850.00
                    }
                ],
                "notes": "Walk-in customer - cash payment"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
            .body(walkInOrderRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.customer", nullValue())
            .body("data.status", equalTo("DRAFT"))
            .body("data.line_items", hasSize(1))
            .body("data.subtotal", equalTo(1700.0f))
    }

    @Test
    @DisplayName("POST /order/v1 - Create jewelry order with precious metals")
    fun `should create jewelry order with weight-based pricing`() {
        val jewelryOrderRequest = """
            {
                "customer_id": "CUST_JEWELRY_001",
                "line_items": [
                    {
                        "product_id": "PROD_GOLD_NECKLACE_001",
                        "quantity": 1,
                        "unit_price": 125000.00
                    },
                    {
                        "product_id": "PROD_SILVER_RING_001",
                        "quantity": 2,
                        "unit_price": 3500.00
                    }
                ],
                "notes": "Custom jewelry order - 22K gold necklace with silver rings"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
            .body(jewelryOrderRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.line_items[0].product.attributes.metal_type", equalTo("GOLD"))
            .body("data.subtotal", equalTo(132000.0f)) // 125000 + (2*3500)
            .body("data.tax_amount", greaterThan(0.0f)) // GST on jewelry
    }

    @Test
    @DisplayName("POST /order/v1 - Validation error for empty line items")
    fun `should return validation error when line items are empty`() {
        val invalidOrderRequest = """
            {
                "customer_id": "CUST_TEST_001",
                "line_items": [],
                "notes": "Invalid order with no line items"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidOrderRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.validation_errors.line_items", containsString("at least one line item required"))
    }

    @Test
    @DisplayName("POST /order/v1 - Validation error for invalid product reference")
    fun `should return error for non-existent product in line items`() {
        val invalidProductOrderRequest = """
            {
                "line_items": [
                    {
                        "product_id": "NONEXISTENT_PRODUCT_999",
                        "quantity": 1,
                        "unit_price": 100.00
                    }
                ]
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidProductOrderRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("PRODUCT_NOT_FOUND"))
    }

    @Test
    @DisplayName("POST /order/v1 - Insufficient inventory validation")
    fun `should return error when insufficient stock for order quantity`() {
        val insufficientStockOrderRequest = """
            {
                "line_items": [
                    {
                        "product_id": "PROD_LOW_STOCK_001",
                        "quantity": 1000,
                        "unit_price": 50.00
                    }
                ],
                "notes": "Order exceeding available stock"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(insufficientStockOrderRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(409)
            .body("success", equalTo(false))
            .body("error.code", equalTo("INSUFFICIENT_INVENTORY"))
            .body("error.message", containsString("insufficient stock"))
    }

    @Test
    @DisplayName("POST /order/v1 - Automatic tax calculation")
    fun `should automatically calculate taxes based on product tax codes`() {
        val taxableOrderRequest = """
            {
                "customer_id": "CUST_GST_001",
                "line_items": [
                    {
                        "product_id": "PROD_GST_18_001",
                        "quantity": 1,
                        "unit_price": 1000.00
                    },
                    {
                        "product_id": "PROD_GST_5_001", 
                        "quantity": 2,
                        "unit_price": 500.00
                    }
                ]
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(taxableOrderRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.line_items[0].tax_rate", equalTo(18.0f))
            .body("data.line_items[0].tax_amount", equalTo(180.0f)) // 18% of 1000
            .body("data.line_items[1].tax_rate", equalTo(5.0f))
            .body("data.line_items[1].tax_amount", equalTo(50.0f)) // 5% of 1000 (2*500)
            .body("data.tax_amount", equalTo(230.0f)) // 180 + 50
    }

    @Test
    @DisplayName("POST /order/v1 - Order number generation")
    fun `should generate unique order number following workspace pattern`() {
        val orderRequest = """
            {
                "line_items": [
                    {
                        "product_id": "PROD_TEST_001",
                        "quantity": 1,
                        "unit_price": 100.00
                    }
                ]
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(orderRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.order_number", matchesRegex("ORD-\\d{8}-\\d{3}")) // Format: ORD-YYYYMMDD-001
    }

    @Test
    @DisplayName("POST /order/v1 - Multi-tenant isolation for customer reference")
    fun `should prevent referencing customer from different workspace`() {
        val crossTenantOrderRequest = """
            {
                "customer_id": "CUST_FROM_OTHER_WORKSPACE",
                "line_items": [
                    {
                        "product_id": "PROD_TEST_001",
                        "quantity": 1,
                        "unit_price": 100.00
                    }
                ]
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_A")
            .body(crossTenantOrderRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("CUSTOMER_NOT_FOUND"))
    }

    @Test
    @DisplayName("POST /order/v1 - Line item discount validation")
    fun `should validate line item discounts do not exceed line total`() {
        val excessiveDiscountRequest = """
            {
                "line_items": [
                    {
                        "product_id": "PROD_TEST_001",
                        "quantity": 1,
                        "unit_price": 100.00,
                        "discount_amount": 150.00
                    }
                ]
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(excessiveDiscountRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.message", containsString("discount cannot exceed line total"))
    }

    @Test
    @DisplayName("POST /order/v1 - Created by user tracking")
    fun `should track user who created the order`() {
        val orderRequest = """
            {
                "line_items": [
                    {
                        "product_id": "PROD_TEST_001",
                        "quantity": 1,
                        "unit_price": 100.00
                    }
                ]
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(orderRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.created_by.id", notNullValue())
            .body("data.created_by.name", notNullValue())
    }

    @Test
    @DisplayName("POST /order/v1 - Minimum quantity validation")
    fun `should validate minimum quantities for line items`() {
        val zeroQuantityRequest = """
            {
                "line_items": [
                    {
                        "product_id": "PROD_TEST_001",
                        "quantity": 0,
                        "unit_price": 100.00
                    }
                ]
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(zeroQuantityRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.validation_errors.quantity", containsString("must be greater than 0"))
    }

    @Test
    @DisplayName("POST /order/v1 - Order with decimal quantities for weight-based products")
    fun `should support decimal quantities for weight-based products`() {
        val decimalQuantityRequest = """
            {
                "customer_id": "CUST_JEWELRY_001",
                "line_items": [
                    {
                        "product_id": "PROD_GOLD_BY_WEIGHT",
                        "quantity": 15.5,
                        "unit_price": 5500.00
                    }
                ],
                "notes": "Gold purchase - 15.5 grams"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
            .body(decimalQuantityRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.line_items[0].quantity", equalTo(15.5f))
            .body("data.line_items[0].line_total", equalTo(85250.0f)) // 15.5 * 5500
    }
}