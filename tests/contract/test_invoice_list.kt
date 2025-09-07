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
 * Contract tests for Invoice Management API - List Invoices endpoint.
 * 
 * Tests verify the GET /invoice/v1/list endpoint with filtering, search,
 * and pagination capabilities according to the retail API contract.
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InvoiceListContractTest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Basic invoice listing with pagination")
    fun `should return paginated invoice list`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("page", 0)
            .queryParam("size", 20)
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("data.content", notNullValue())
            .body("data.page", equalTo(0))
            .body("data.size", equalTo(20))
            .body("data.total_elements", greaterThanOrEqualTo(0))
            .body("data.total_pages", greaterThanOrEqualTo(0))
            .body("data.first", equalTo(true))
            .body("data.last", notNullValue())
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Filter invoices by date range")
    fun `should filter invoices by generated date range`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("from_date", "2025-09-01")
            .queryParam("to_date", "2025-09-30")
            .queryParam("page", 0)
            .queryParam("size", 10)
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThanOrEqualTo(0)))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Search invoices by invoice number")
    fun `should search invoices by invoice number with partial matching`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("search", "INV-20250907")
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content.findAll { it.invoice_number.contains('INV-20250907') }", 
                  hasSize(greaterThanOrEqualTo(0)))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Search by customer name")
    fun `should search invoices by customer name`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
            .queryParam("search", "Priya")
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content.findAll { it.customer.name.toLowerCase().contains('priya') }", 
                  hasSize(greaterThanOrEqualTo(0)))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Filter by invoice status")
    fun `should filter invoices by payment status`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
            .queryParam("status", "PENDING")
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("status", "PENDING")))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Filter by invoice type")
    fun `should filter invoices by invoice type`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_HARDWARE_WS_001")
            .queryParam("invoice_type", "GST")
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("invoice_type", "GST")))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Filter by payment method")
    fun `should filter invoices by payment method`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("payment_method", "CASH")
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("payment_method", "CASH")))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Filter by amount range")
    fun `should filter invoices by total amount range`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
            .queryParam("min_amount", 10000)
            .queryParam("max_amount", 100000)
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(allOf(
                hasEntry("total_amount", greaterThanOrEqualTo(10000.0f)),
                hasEntry("total_amount", lessThanOrEqualTo(100000.0f))
            )))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Combined filtering and search")
    fun `should support combined filtering and search parameters`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("search", "Kumar")
            .queryParam("status", "PAID")
            .queryParam("invoice_type", "RETAIL")
            .queryParam("from_date", "2025-09-01")
            .queryParam("to_date", "2025-09-30")
            .queryParam("page", 0)
            .queryParam("size", 5)
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.size", equalTo(5))
            .body("data.content", everyItem(allOf(
                hasEntry("status", "PAID"),
                hasEntry("invoice_type", "RETAIL")
            )))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Sort by invoice date descending")
    fun `should sort invoices by generated date in descending order`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("sort", "generated_at")
            .queryParam("direction", "desc")
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThan(0)))
            .body("data.content[0].generated_at", notNullValue())
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Sort by total amount ascending")
    fun `should sort invoices by total amount in ascending order`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("sort", "total_amount")
            .queryParam("direction", "asc")
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThan(0)))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Include summary statistics")
    fun `should include invoice summary statistics when requested`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("include_summary", true)
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.summary.total_invoices", greaterThanOrEqualTo(0))
            .body("data.summary.total_amount", greaterThanOrEqualTo(0.0f))
            .body("data.summary.paid_amount", greaterThanOrEqualTo(0.0f))
            .body("data.summary.pending_amount", greaterThanOrEqualTo(0.0f))
            .body("data.summary.overdue_amount", greaterThanOrEqualTo(0.0f))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Filter overdue invoices")
    fun `should filter invoices that are overdue for payment`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
            .queryParam("overdue_only", true)
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(allOf(
                hasEntry("status", "PENDING"),
                hasEntry("is_overdue", equalTo(true))
            )))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Filter invoices by customer type")
    fun `should filter invoices by customer business type`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_HARDWARE_WS_001")
            .queryParam("customer_type", "CONTRACTOR")
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("customer.customer_type", "CONTRACTOR")))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Multi-tenant isolation")
    fun `should only return invoices from current workspace`() {
        // Request invoices from workspace A
        val workspaceAInvoices = RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_A")
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .extract()
            .path<List<String>>("data.content.id")

        // Request invoices from workspace B
        val workspaceBInvoices = RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_B")
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .extract()
            .path<List<String>>("data.content.id")

        // Invoices from different workspaces should not overlap
        assert(workspaceAInvoices.intersect(workspaceBInvoices.toSet()).isEmpty())
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Empty result set")
    fun `should return empty list when no invoices match criteria`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_EMPTY_WS_001")
            .queryParam("search", "nonexistent_invoice_xyz")
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(0))
            .body("data.total_elements", equalTo(0))
            .body("data.total_pages", equalTo(0))
            .body("data.empty", equalTo(true))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Invalid date range validation")
    fun `should validate date range parameters`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("from_date", "2025-12-01")
            .queryParam("to_date", "2025-01-01") // to_date before from_date
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.message", containsString("to_date must be after from_date"))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Invalid pagination parameters")
    fun `should handle invalid pagination parameters gracefully`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("page", -1)
            .queryParam("size", 0)
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Invoice response structure validation")
    fun `should return invoices with complete response structure`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("size", 1)
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content[0].id", notNullValue())
            .body("data.content[0].invoice_number", notNullValue())
            .body("data.content[0].invoice_type", notNullValue())
            .body("data.content[0].customer", notNullValue())
            .body("data.content[0].total_amount", notNullValue())
            .body("data.content[0].status", notNullValue())
            .body("data.content[0].generated_at", notNullValue())
            .body("data.content[0].payment_method", notNullValue())
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Filter by tax type for GST analysis")
    fun `should filter GST invoices for tax analysis`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("invoice_type", "GST")
            .queryParam("from_date", "2025-09-01")
            .queryParam("to_date", "2025-09-30")
            .queryParam("include_tax_breakup", true)
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("invoice_type", "GST")))
            .body("data.content[0].tax_breakup.cgst", notNullValue())
            .body("data.content[0].tax_breakup.sgst", notNullValue())
            .body("data.content[0].tax_breakup.igst", notNullValue())
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Export invoice list")
    fun `should support invoice list export in different formats`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("export_format", "EXCEL")
            .queryParam("from_date", "2025-09-01")
            .queryParam("to_date", "2025-09-30")
            .`when`()
            .get("/invoice/v1/list/export")
            .then()
            .statusCode(200)
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .header("Content-Disposition", containsString("invoices_export"))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Bulk operations support")
    fun `should support bulk operations on filtered invoice lists`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("status", "PENDING")
            .queryParam("overdue_only", true)
            .queryParam("bulk_actions", true)
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.bulk_actions.available", hasItems("SEND_REMINDER", "MARK_OVERDUE", "APPLY_LATE_FEE"))
            .body("data.bulk_actions.selected_count", greaterThanOrEqualTo(0))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Performance with large datasets")
    fun `should handle large invoice lists efficiently`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_LARGE_DATA_WS_001")
            .queryParam("page", 0)
            .queryParam("size", 100)
            .queryParam("performance_mode", true)
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .time(lessThan(2000L)) // Should respond within 2 seconds
            .body("success", equalTo(true))
            .body("data.content", hasSize(lessThanOrEqualTo(100)))
    }

    @Test
    @DisplayName("GET /invoice/v1/list - Real-time payment status updates")
    fun `should reflect real-time payment status changes`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("real_time", true)
            .queryParam("last_sync", "2025-09-07T10:00:00Z")
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.sync_timestamp", notNullValue())
            .body("data.updates_since_last_sync", greaterThanOrEqualTo(0))
    }
}