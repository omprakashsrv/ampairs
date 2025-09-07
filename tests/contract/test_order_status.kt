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
 * Contract tests for Order Status Management API.
 * 
 * Tests verify order status workflow transitions via PUT /order/v1/{id}/status
 * according to the retail API contract with proper state machine validation.
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderStatusContractTest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @DisplayName("PUT /order/v1/{id}/status - Transition DRAFT to CONFIRMED")
    fun `should transition order from draft to confirmed status`() {
        val statusUpdateRequest = """
            {
                "new_status": "CONFIRMED",
                "notes": "Customer confirmed the order via phone"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(statusUpdateRequest)
            .`when`()
            .put("/order/v1/ORD-20250907-001/status")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("data.id", equalTo("ORD-20250907-001"))
            .body("data.status", equalTo("CONFIRMED"))
            .body("data.status_history", hasSize(greaterThan(1)))
            .body("data.status_history[0].from_status", equalTo("DRAFT"))
            .body("data.status_history[0].to_status", equalTo("CONFIRMED"))
            .body("data.status_history[0].changed_by.name", notNullValue())
            .body("data.status_history[0].timestamp", notNullValue())
            .body("data.status_history[0].notes", equalTo("Customer confirmed the order via phone"))
    }

    @Test
    @DisplayName("PUT /order/v1/{id}/status - Transition CONFIRMED to PROCESSING")
    fun `should transition order from confirmed to processing status`() {
        val statusUpdateRequest = """
            {
                "new_status": "PROCESSING",
                "notes": "Started preparing order items"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_HARDWARE_WS_001")
            .body(statusUpdateRequest)
            .`when`()
            .put("/order/v1/ORD-20250907-002/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("PROCESSING"))
            .body("data.processing_started_at", notNullValue())
            .body("data.estimated_completion_time", notNullValue())
    }

    @Test
    @DisplayName("PUT /order/v1/{id}/status - Transition PROCESSING to READY")
    fun `should transition order from processing to ready for pickup`() {
        val statusUpdateRequest = """
            {
                "new_status": "READY",
                "notes": "All items packed and ready for customer pickup"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
            .body(statusUpdateRequest)
            .`when`()
            .put("/order/v1/ORD-20250907-003/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("READY"))
            .body("data.ready_at", notNullValue())
            .body("data.pickup_instructions", notNullValue())
    }

    @Test
    @DisplayName("PUT /order/v1/{id}/status - Transition READY to COMPLETED")
    fun `should complete order when customer picks up items`() {
        val statusUpdateRequest = """
            {
                "new_status": "COMPLETED",
                "notes": "Customer picked up order and paid in cash",
                "payment_method": "CASH",
                "payment_amount": 1500.00
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
            .body(statusUpdateRequest)
            .`when`()
            .put("/order/v1/ORD-20250907-004/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("COMPLETED"))
            .body("data.completed_at", notNullValue())
            .body("data.payment_method", equalTo("CASH"))
            .body("data.payment_amount", equalTo(1500.0f))
            .body("data.invoice_generated", equalTo(true))
    }

    @Test
    @DisplayName("PUT /order/v1/{id}/status - Cancel order from DRAFT status")
    fun `should cancel order from draft status`() {
        val statusUpdateRequest = """
            {
                "new_status": "CANCELLED",
                "cancellation_reason": "CUSTOMER_REQUEST",
                "notes": "Customer no longer needs the items"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(statusUpdateRequest)
            .`when`()
            .put("/order/v1/ORD-20250907-005/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("CANCELLED"))
            .body("data.cancellation_reason", equalTo("CUSTOMER_REQUEST"))
            .body("data.cancelled_at", notNullValue())
            .body("data.inventory_released", equalTo(true))
    }

    @Test
    @DisplayName("PUT /order/v1/{id}/status - Cancel order from CONFIRMED status with restocking")
    fun `should cancel confirmed order and release reserved inventory`() {
        val statusUpdateRequest = """
            {
                "new_status": "CANCELLED",
                "cancellation_reason": "OUT_OF_STOCK",
                "notes": "Supplier could not deliver key components"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_HARDWARE_WS_001")
            .body(statusUpdateRequest)
            .`when`()
            .put("/order/v1/ORD-20250907-006/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("CANCELLED"))
            .body("data.cancellation_reason", equalTo("OUT_OF_STOCK"))
            .body("data.inventory_released", equalTo(true))
            .body("data.refund_required", equalTo(true))
    }

    @Test
    @DisplayName("PUT /order/v1/{id}/status - Invalid status transition validation")
    fun `should reject invalid status transitions`() {
        val invalidStatusUpdateRequest = """
            {
                "new_status": "COMPLETED",
                "notes": "Trying to skip workflow steps"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidStatusUpdateRequest)
            .`when`()
            .put("/order/v1/ORD-DRAFT-001/status")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("INVALID_STATUS_TRANSITION"))
            .body("error.message", containsString("cannot transition from DRAFT to COMPLETED"))
            .body("error.details.allowed_transitions", hasItems("CONFIRMED", "CANCELLED"))
    }

    @Test
    @DisplayName("PUT /order/v1/{id}/status - Prevent transition from completed order")
    fun `should prevent status changes on completed orders`() {
        val statusUpdateRequest = """
            {
                "new_status": "PROCESSING",
                "notes": "Trying to modify completed order"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(statusUpdateRequest)
            .`when`()
            .put("/order/v1/ORD-COMPLETED-001/status")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("ORDER_ALREADY_FINALIZED"))
            .body("error.message", containsString("completed orders cannot be modified"))
    }

    @Test
    @DisplayName("PUT /order/v1/{id}/status - Order not found validation")
    fun `should return not found for non-existent order`() {
        val statusUpdateRequest = """
            {
                "new_status": "CONFIRMED",
                "notes": "Status update for non-existent order"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(statusUpdateRequest)
            .`when`()
            .put("/order/v1/NONEXISTENT-ORDER-999/status")
            .then()
            .statusCode(404)
            .body("success", equalTo(false))
            .body("error.code", equalTo("ORDER_NOT_FOUND"))
    }

    @Test
    @DisplayName("PUT /order/v1/{id}/status - Multi-tenant isolation validation")
    fun `should prevent status updates across workspace boundaries`() {
        val statusUpdateRequest = """
            {
                "new_status": "CONFIRMED",
                "notes": "Cross-tenant status update attempt"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_A")
            .body(statusUpdateRequest)
            .`when`()
            .put("/order/v1/ORD-FROM-WORKSPACE-B/status")
            .then()
            .statusCode(404)
            .body("error.code", equalTo("ORDER_NOT_FOUND"))
    }

    @Test
    @DisplayName("PUT /order/v1/{id}/status - Required fields validation")
    fun `should validate required fields for status updates`() {
        val invalidStatusUpdateRequest = """
            {
                "notes": "Missing new_status field"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidStatusUpdateRequest)
            .`when`()
            .put("/order/v1/ORD-20250907-001/status")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.validation_errors.new_status", containsString("required"))
    }

    @Test
    @DisplayName("PUT /order/v1/{id}/status - Invalid status value validation")
    fun `should reject invalid status values`() {
        val invalidStatusUpdateRequest = """
            {
                "new_status": "INVALID_STATUS",
                "notes": "Testing invalid status value"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidStatusUpdateRequest)
            .`when`()
            .put("/order/v1/ORD-20250907-001/status")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.validation_errors.new_status", containsString("must be one of: DRAFT, CONFIRMED, PROCESSING, READY, COMPLETED, CANCELLED"))
    }

    @Test
    @DisplayName("PUT /order/v1/{id}/status - Bulk status update for multiple orders")
    fun `should support bulk status updates for order batches`() {
        val bulkStatusUpdateRequest = """
            {
                "order_ids": ["ORD-20250907-010", "ORD-20250907-011", "ORD-20250907-012"],
                "new_status": "PROCESSING",
                "notes": "Bulk processing start for morning batch"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(bulkStatusUpdateRequest)
            .`when`()
            .put("/order/v1/bulk/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.updated_count", equalTo(3))
            .body("data.failed_updates", hasSize(0))
            .body("data.results", hasSize(3))
            .body("data.results[0].order_id", equalTo("ORD-20250907-010"))
            .body("data.results[0].status", equalTo("PROCESSING"))
    }

    @Test
    @DisplayName("PUT /order/v1/{id}/status - Status history tracking")
    fun `should maintain complete status transition history`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .`when`()
            .get("/order/v1/ORD-WITH-HISTORY-001")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status_history", hasSize(greaterThan(0)))
            .body("data.status_history[0].from_status", notNullValue())
            .body("data.status_history[0].to_status", notNullValue())
            .body("data.status_history[0].changed_by.id", notNullValue())
            .body("data.status_history[0].changed_by.name", notNullValue())
            .body("data.status_history[0].timestamp", notNullValue())
            .body("data.status_history[0].duration_minutes", greaterThanOrEqualTo(0))
    }

    @Test
    @DisplayName("PUT /order/v1/{id}/status - Automatic inventory reservation for confirmed orders")
    fun `should automatically reserve inventory when order is confirmed`() {
        val statusUpdateRequest = """
            {
                "new_status": "CONFIRMED",
                "notes": "Confirming order with inventory reservation"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(statusUpdateRequest)
            .`when`()
            .put("/order/v1/ORD-INVENTORY-TEST-001/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("CONFIRMED"))
            .body("data.inventory_reserved", equalTo(true))
            .body("data.reservation_details", hasSize(greaterThan(0)))
            .body("data.reservation_details[0].product_id", notNullValue())
            .body("data.reservation_details[0].quantity_reserved", greaterThan(0.0f))
            .body("data.reservation_details[0].reserved_until", notNullValue())
    }
}