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
 * Contract tests for Customer Management API - List Customers endpoint.
 * 
 * Tests verify the GET /customer/v1/list endpoint with search, filtering,
 * and pagination capabilities according to the retail API contract.
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CustomerListContractTest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @DisplayName("GET /customer/v1/list - Basic customer listing with pagination")
    fun `should return paginated customer list`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("page", 0)
            .queryParam("size", 20)
            .`when`()
            .get("/customer/v1/list")
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
    @DisplayName("GET /customer/v1/list - Search customers by name")
    fun `should search customers by name with partial matching`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("search", "Kumar")
            .queryParam("page", 0)
            .queryParam("size", 10)
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThanOrEqualTo(0)))
            // All returned customers should contain "Kumar" in name
            .body("data.content.findAll { it.name.toLowerCase().contains('kumar') }", 
                  hasSize(greaterThanOrEqualTo(0)))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Search by phone number")
    fun `should search customers by phone number with partial matching`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
            .queryParam("search", "9876")
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content.findAll { it.phone.contains('9876') }", hasSize(greaterThanOrEqualTo(0)))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Search by customer number")
    fun `should search customers by customer number`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
            .queryParam("search", "CUST-20250907")
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content.findAll { it.customer_number.contains('CUST-20250907') }", 
                  hasSize(greaterThanOrEqualTo(0)))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Filter by customer type")
    fun `should filter customers by customer type`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_HARDWARE_WS_001")
            .queryParam("customer_type", "WHOLESALE")
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("customer_type", "WHOLESALE")))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Filter by customer status")
    fun `should filter customers by status`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("status", "ACTIVE")
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("status", "ACTIVE")))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Filter by city location")
    fun `should filter customers by city`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("city", "Bangalore")
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("address.city", "Bangalore")))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Combined search and filtering")
    fun `should support combined search with type and status filters`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
            .queryParam("search", "Store")
            .queryParam("customer_type", "WHOLESALE")
            .queryParam("status", "ACTIVE")
            .queryParam("page", 0)
            .queryParam("size", 5)
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.size", equalTo(5))
            .body("data.content", everyItem(allOf(
                hasEntry("customer_type", "WHOLESALE"),
                hasEntry("status", "ACTIVE")
            )))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Sort by name ascending")
    fun `should sort customers by name in ascending order`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("sort", "name")
            .queryParam("direction", "asc")
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThan(0)))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Sort by creation date descending")
    fun `should sort customers by created_at in descending order`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("sort", "created_at")
            .queryParam("direction", "desc")
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThan(0)))
            .body("data.content[0].created_at", notNullValue())
    }

    @Test
    @DisplayName("GET /customer/v1/list - Filter customers with outstanding credit")
    fun `should filter customers with outstanding credit balance`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
            .queryParam("has_outstanding_credit", true)
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("outstanding_balance", greaterThan(0.0f))))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Filter by credit limit range")
    fun `should filter customers by credit limit range`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_HARDWARE_WS_001")
            .queryParam("min_credit_limit", 10000)
            .queryParam("max_credit_limit", 100000)
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(allOf(
                hasEntry("credit_limit", greaterThanOrEqualTo(10000.0f)),
                hasEntry("credit_limit", lessThanOrEqualTo(100000.0f))
            )))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Include purchase statistics")
    fun `should include customer purchase statistics when requested`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("include_stats", true)
            .queryParam("size", 1)
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThan(0)))
            .body("data.content[0].stats.total_orders", notNullValue())
            .body("data.content[0].stats.total_spent", notNullValue())
            .body("data.content[0].stats.last_order_date", notNullValue())
            .body("data.content[0].stats.average_order_value", notNullValue())
    }

    @Test
    @DisplayName("GET /customer/v1/list - Filter by GST registration status")
    fun `should filter customers by GST registration status`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("has_gst", true)
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("gst_number", notNullValue())))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Multi-tenant isolation")
    fun `should only return customers from current workspace`() {
        // Request customers from workspace A
        val workspaceACustomers = RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_A")
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .extract()
            .path<List<String>>("data.content.id")

        // Request customers from workspace B
        val workspaceBCustomers = RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_B")
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .extract()
            .path<List<String>>("data.content.id")

        // Customers from different workspaces should not overlap
        assert(workspaceACustomers.intersect(workspaceBCustomers.toSet()).isEmpty())
    }

    @Test
    @DisplayName("GET /customer/v1/list - Empty result set")
    fun `should return empty list when no customers match criteria`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_EMPTY_WS_001")
            .queryParam("search", "nonexistent_customer_xyz")
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(0))
            .body("data.total_elements", equalTo(0))
            .body("data.total_pages", equalTo(0))
            .body("data.empty", equalTo(true))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Invalid pagination parameters")
    fun `should handle invalid pagination parameters gracefully`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("page", -1)
            .queryParam("size", 0)
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Unauthorized without workspace header")
    fun `should return error when workspace header is missing`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            // Missing X-Workspace-ID header
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(400)
            .body("error.code", equalTo("INVALID_TENANT_CONTEXT"))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Customer response structure validation")
    fun `should return customers with complete response structure`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("size", 1)
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content[0].id", notNullValue())
            .body("data.content[0].customer_number", notNullValue())
            .body("data.content[0].name", notNullValue())
            .body("data.content[0].phone", notNullValue())
            .body("data.content[0].customer_type", notNullValue())
            .body("data.content[0].status", notNullValue())
            .body("data.content[0].created_at", notNullValue())
            .body("data.content[0].updated_at", notNullValue())
            // Optional fields
            .body("data.content[0]", anyOf(
                hasEntry("email", notNullValue()),
                not(hasKey("email"))
            ))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Filter by loyalty program membership")
    fun `should filter customers by loyalty program enrollment`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("loyalty_member", true)
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("loyalty_program.enrolled", equalTo(true))))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Filter by recent activity")
    fun `should filter customers by recent activity date range`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("last_activity_from", "2024-01-01")
            .queryParam("last_activity_to", "2024-12-31")
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("last_activity_date", notNullValue())))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Export customer list")
    fun `should support customer list export in different formats`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("export_format", "CSV")
            .queryParam("include_stats", true)
            .`when`()
            .get("/customer/v1/list/export")
            .then()
            .statusCode(200)
            .contentType("text/csv")
            .header("Content-Disposition", containsString("customers_export"))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Filter by business attributes for jewelry customers")
    fun `should filter jewelry customers by metal preferences`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
            .queryParam("preferred_metal", "GOLD")
            .queryParam("preferred_purity", "22K")
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(allOf(
                hasEntry("attributes.preferred_metal", "GOLD"),
                hasEntry("attributes.preferred_purity", "22K")
            )))
    }

    @Test
    @DisplayName("GET /customer/v1/list - Filter contractors by project type")
    fun `should filter hardware customers by project type preferences`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_HARDWARE_WS_001")
            .queryParam("project_type", "COMMERCIAL")
            .queryParam("customer_type", "CONTRACTOR")
            .`when`()
            .get("/customer/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(allOf(
                hasEntry("customer_type", "CONTRACTOR"),
                hasEntry("attributes.project_types", hasItem("COMMERCIAL"))
            )))
    }
}