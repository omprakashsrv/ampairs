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
 * Contract tests for Product Inventory Management API.
 * 
 * Tests verify inventory operations: GET/PUT /product/v1/{productId}/inventory
 * according to the retail API contract with stock adjustments and movement tracking.
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductInventoryContractTest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @DisplayName("GET /product/v1/{productId}/inventory - Get inventory details")
    fun `should return complete inventory information for product`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .`when`()
            .get("/product/v1/PROD_TEST_001/inventory")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("data.product_id", equalTo("PROD_TEST_001"))
            .body("data.current_stock", notNullValue())
            .body("data.reserved_stock", greaterThanOrEqualTo(0.0f))
            .body("data.available_stock", notNullValue())
            .body("data.reorder_level", notNullValue())
            .body("data.max_stock_level", notNullValue())
            .body("data.last_updated", notNullValue())
            .body("data.recent_movements", notNullValue())
    }

    @Test
    @DisplayName("PUT /product/v1/{productId}/inventory - Set initial stock")
    fun `should set initial inventory levels for new product`() {
        val inventoryRequest = """
            {
                "adjustment_type": "SET",
                "quantity": 100.0,
                "reason": "Initial stock setup",
                "reorder_level": 10.0,
                "max_stock_level": 500.0
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_HARDWARE_WS_001")
            .body(inventoryRequest)
            .`when`()
            .put("/product/v1/PROD_NEW_001/inventory")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.current_stock", equalTo(100.0f))
            .body("data.available_stock", equalTo(100.0f))
            .body("data.reserved_stock", equalTo(0.0f))
            .body("data.reorder_level", equalTo(10.0f))
            .body("data.max_stock_level", equalTo(500.0f))
    }

    @Test
    @DisplayName("PUT /product/v1/{productId}/inventory - Add stock (receiving)")
    fun `should add stock to existing inventory`() {
        val addStockRequest = """
            {
                "adjustment_type": "ADD",
                "quantity": 50.0,
                "reason": "Stock received from supplier"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
            .body(addStockRequest)
            .`when`()
            .put("/product/v1/PROD_RICE_001/inventory")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.current_stock", greaterThan(50.0f))
            .body("data.recent_movements", hasSize(greaterThan(0)))
            .body("data.recent_movements[0].movement_type", equalTo("IN"))
            .body("data.recent_movements[0].quantity", equalTo(50.0f))
            .body("data.recent_movements[0].reason", equalTo("Stock received from supplier"))
    }

    @Test
    @DisplayName("PUT /product/v1/{productId}/inventory - Subtract stock (sales/damage)")
    fun `should subtract stock from inventory`() {
        val subtractStockRequest = """
            {
                "adjustment_type": "SUBTRACT",
                "quantity": 25.0,
                "reason": "Stock sold to customer"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
            .body(subtractStockRequest)
            .`when`()
            .put("/product/v1/PROD_GOLD_RING_001/inventory")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.recent_movements[0].movement_type", equalTo("OUT"))
            .body("data.recent_movements[0].quantity", equalTo(25.0f))
            .body("data.recent_movements[0].reason", equalTo("Stock sold to customer"))
    }

    @Test
    @DisplayName("PUT /product/v1/{productId}/inventory - Stock adjustment for corrections")
    fun `should handle inventory adjustments for corrections`() {
        val adjustmentRequest = """
            {
                "adjustment_type": "SET",
                "quantity": 75.0,
                "reason": "Physical stock count correction"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(adjustmentRequest)
            .`when`()
            .put("/product/v1/PROD_HAMMER_001/inventory")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.current_stock", equalTo(75.0f))
            .body("data.recent_movements[0].movement_type", equalTo("ADJUSTMENT"))
            .body("data.recent_movements[0].reason", equalTo("Physical stock count correction"))
    }

    @Test
    @DisplayName("PUT /product/v1/{productId}/inventory - Validation error for negative stock")
    fun `should prevent setting negative stock levels`() {
        val negativeStockRequest = """
            {
                "adjustment_type": "SET",
                "quantity": -10.0,
                "reason": "Invalid negative stock"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(negativeStockRequest)
            .`when`()
            .put("/product/v1/PROD_TEST_001/inventory")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.validation_errors.quantity", containsString("must be positive"))
    }

    @Test
    @DisplayName("PUT /product/v1/{productId}/inventory - Validation error when subtracting more than available")
    fun `should prevent stock going below zero during subtraction`() {
        val excessiveSubtractRequest = """
            {
                "adjustment_type": "SUBTRACT",
                "quantity": 1000.0,
                "reason": "Attempting to subtract more than available"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(excessiveSubtractRequest)
            .`when`()
            .put("/product/v1/PROD_LOW_STOCK_001/inventory")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("INSUFFICIENT_STOCK"))
            .body("error.message", containsString("insufficient stock"))
    }

    @Test
    @DisplayName("GET /product/v1/{productId}/inventory - Include movement history")
    fun `should return recent inventory movements with details`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("include_movements", true)
            .queryParam("movement_limit", 10)
            .`when`()
            .get("/product/v1/PROD_ACTIVE_001/inventory")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.recent_movements", hasSize(lessThanOrEqualTo(10)))
            .body("data.recent_movements[0].id", notNullValue())
            .body("data.recent_movements[0].movement_type", isIn(listOf("IN", "OUT", "ADJUSTMENT", "TRANSFER")))
            .body("data.recent_movements[0].quantity", notNullValue())
            .body("data.recent_movements[0].previous_stock", notNullValue())
            .body("data.recent_movements[0].new_stock", notNullValue())
            .body("data.recent_movements[0].timestamp", notNullValue())
            .body("data.recent_movements[0].user_name", notNullValue())
    }

    @Test
    @DisplayName("PUT /product/v1/{productId}/inventory - Update reorder levels")
    fun `should update reorder and maximum stock levels`() {
        val updateLevelsRequest = """
            {
                "adjustment_type": "SET",
                "quantity": 200.0,
                "reorder_level": 25.0,
                "max_stock_level": 1000.0,
                "reason": "Updated stock management policies"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(updateLevelsRequest)
            .`when`()
            .put("/product/v1/PROD_TEST_001/inventory")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.current_stock", equalTo(200.0f))
            .body("data.reorder_level", equalTo(25.0f))
            .body("data.max_stock_level", equalTo(1000.0f))
    }

    @Test
    @DisplayName("GET /product/v1/{productId}/inventory - Low stock detection")
    fun `should indicate when stock is below reorder level`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .`when`()
            .get("/product/v1/PROD_LOW_STOCK_001/inventory")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.is_low_stock", equalTo(true))
            .body("data.current_stock", lessThanOrEqualTo(Float.parseFloat("${RestAssured.get().path("data.reorder_level")}")))
    }

    @Test
    @DisplayName("PUT /product/v1/{productId}/inventory - Reserved stock management")
    fun `should handle reserved stock for pending orders`() {
        // This test assumes that stock reservation is handled automatically by the system
        // when orders are created, but we can test the reporting
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .`when`()
            .get("/product/v1/PROD_RESERVED_001/inventory")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.current_stock", notNullValue())
            .body("data.reserved_stock", greaterThanOrEqualTo(0.0f))
            .body("data.available_stock", equalTo(
                Float.parseFloat("${RestAssured.get().path("data.current_stock")}") - 
                Float.parseFloat("${RestAssured.get().path("data.reserved_stock")}")
            ))
    }

    @Test
    @DisplayName("GET /product/v1/{productId}/inventory - Product not found")
    fun `should return not found for non-existent product`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .`when`()
            .get("/product/v1/NONEXISTENT_PRODUCT/inventory")
            .then()
            .statusCode(404)
            .body("success", equalTo(false))
            .body("error.code", equalTo("PRODUCT_NOT_FOUND"))
    }

    @Test
    @DisplayName("PUT /product/v1/{productId}/inventory - Multi-tenant isolation")
    fun `should prevent inventory updates across workspace boundaries`() {
        val inventoryRequest = """
            {
                "adjustment_type": "SET",
                "quantity": 999.0,
                "reason": "Cross-tenant test"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_A")
            .body(inventoryRequest)
            .`when`()
            .put("/product/v1/PROD_FROM_WORKSPACE_B/inventory")
            .then()
            .statusCode(404)
            .body("error.code", equalTo("PRODUCT_NOT_FOUND"))
    }

    @Test
    @DisplayName("PUT /product/v1/{productId}/inventory - Missing required fields validation")
    fun `should validate required fields in inventory update`() {
        val invalidRequest = """
            {
                "quantity": 50.0
                // Missing adjustment_type
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidRequest)
            .`when`()
            .put("/product/v1/PROD_TEST_001/inventory")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.validation_errors.adjustment_type", containsString("required"))
    }

    @Test
    @DisplayName("PUT /product/v1/{productId}/inventory - Invalid adjustment type")
    fun `should reject invalid adjustment types`() {
        val invalidAdjustmentRequest = """
            {
                "adjustment_type": "INVALID_TYPE",
                "quantity": 50.0,
                "reason": "Testing invalid adjustment type"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidAdjustmentRequest)
            .`when`()
            .put("/product/v1/PROD_TEST_001/inventory")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.validation_errors.adjustment_type", containsString("must be one of: SET, ADD, SUBTRACT"))
    }
}