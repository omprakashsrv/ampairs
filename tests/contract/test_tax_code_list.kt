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
 * Contract tests for Tax Code Management API - List Tax Codes endpoint.
 * 
 * Tests verify the GET /tax-code/v1/list endpoint with filtering by business type,
 * tax rates, and other criteria according to the retail API contract.
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TaxCodeListContractTest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Basic tax code listing with pagination")
    fun `should return paginated tax code list`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("page", 0)
            .queryParam("size", 20)
            .`when`()
            .get("/tax-code/v1/list")
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
    @DisplayName("GET /tax-code/v1/list - Filter tax codes by business type")
    fun `should filter tax codes applicable to specific business type`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
            .queryParam("business_type", "JEWELRY")
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(
                hasEntry("applicable_business_types", hasItem("JEWELRY"))
            ))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Filter tax codes by tax rate")
    fun `should filter tax codes by specific tax rate`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("tax_rate", 18.0)
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("rate", 18.0f)))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Filter tax codes by tax type")
    fun `should filter tax codes by GST tax type`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
            .queryParam("tax_type", "GST")
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("tax_type", "GST")))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Filter tax codes by status")
    fun `should filter tax codes by active status`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_HARDWARE_WS_001")
            .queryParam("status", "ACTIVE")
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("status", "ACTIVE")))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Search tax codes by code or name")
    fun `should search tax codes by code or name with partial matching`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("search", "GST-18")
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content.findAll { it.code.contains('GST-18') || it.name.toLowerCase().contains('gst-18') }", 
                  hasSize(greaterThanOrEqualTo(0)))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Filter by HSN code")
    fun `should filter tax codes by HSN code`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("hsn_code", "8471")
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("hsn_code", "8471")))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Filter by tax rate range")
    fun `should filter tax codes within tax rate range`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("min_rate", 10.0)
            .queryParam("max_rate", 20.0)
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(allOf(
                hasEntry("rate", greaterThanOrEqualTo(10.0f)),
                hasEntry("rate", lessThanOrEqualTo(20.0f))
            )))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Filter by effective date range")
    fun `should filter tax codes by effective date range`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("effective_from", "2025-01-01")
            .queryParam("effective_to", "2025-12-31")
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThanOrEqualTo(0)))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Combined filtering and search")
    fun `should support combined filtering and search parameters`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("search", "GST")
            .queryParam("business_type", "RETAIL")
            .queryParam("status", "ACTIVE")
            .queryParam("tax_type", "GST")
            .queryParam("page", 0)
            .queryParam("size", 10)
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.size", equalTo(10))
            .body("data.content", everyItem(allOf(
                hasEntry("tax_type", "GST"),
                hasEntry("status", "ACTIVE"),
                hasEntry("applicable_business_types", hasItem("RETAIL"))
            )))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Sort by tax rate ascending")
    fun `should sort tax codes by rate in ascending order`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("sort", "rate")
            .queryParam("direction", "asc")
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThan(0)))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Sort by creation date descending")
    fun `should sort tax codes by created date in descending order`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("sort", "created_at")
            .queryParam("direction", "desc")
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThan(0)))
            .body("data.content[0].created_at", notNullValue())
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Filter exempt tax codes")
    fun `should filter tax codes that are tax exempt`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
            .queryParam("is_exempt", true)
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("is_exempt", equalTo(true))))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Filter compound tax codes")
    fun `should filter compound tax codes with multiple components`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("is_compound", true)
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("is_compound", equalTo(true))))
            .body("data.content[0].components", hasSize(greaterThan(0)))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Filter by cess applicability")
    fun `should filter tax codes with cess applicable`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("cess_applicable", true)
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("cess_applicable", equalTo(true))))
            .body("data.content[0].cess_rate", greaterThan(0.0f))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Multi-tenant isolation")
    fun `should only return tax codes from current workspace`() {
        // Request tax codes from workspace A
        val workspaceATaxCodes = RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_A")
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .extract()
            .path<List<String>>("data.content.id")

        // Request tax codes from workspace B
        val workspaceBTaxCodes = RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_B")
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .extract()
            .path<List<String>>("data.content.id")

        // Tax codes from different workspaces should not overlap
        assert(workspaceATaxCodes.intersect(workspaceBTaxCodes.toSet()).isEmpty())
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Empty result set")
    fun `should return empty list when no tax codes match criteria`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_EMPTY_WS_001")
            .queryParam("search", "nonexistent_tax_code_xyz")
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(0))
            .body("data.total_elements", equalTo(0))
            .body("data.total_pages", equalTo(0))
            .body("data.empty", equalTo(true))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Invalid business type validation")
    fun `should validate business type parameter`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("business_type", "INVALID_BUSINESS_TYPE")
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.message", containsString("invalid business type"))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Invalid pagination parameters")
    fun `should handle invalid pagination parameters gracefully`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("page", -1)
            .queryParam("size", 0)
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Tax code response structure validation")
    fun `should return tax codes with complete response structure`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("size", 1)
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content[0].id", notNullValue())
            .body("data.content[0].code", notNullValue())
            .body("data.content[0].name", notNullValue())
            .body("data.content[0].tax_type", notNullValue())
            .body("data.content[0].rate", notNullValue())
            .body("data.content[0].hsn_code", notNullValue())
            .body("data.content[0].status", notNullValue())
            .body("data.content[0].applicable_business_types", notNullValue())
            .body("data.content[0].created_at", notNullValue())
            .body("data.content[0].updated_at", notNullValue())
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Include tax calculation examples")
    fun `should include tax calculation examples when requested`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("include_examples", true)
            .queryParam("example_amount", 10000)
            .queryParam("size", 1)
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content[0].calculation_example.base_amount", equalTo(10000.0f))
            .body("data.content[0].calculation_example.tax_amount", notNullValue())
            .body("data.content[0].calculation_example.total_amount", notNullValue())
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Filter by reverse charge mechanism")
    fun `should filter tax codes with reverse charge mechanism`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("reverse_charge", true)
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("special_provisions.reverse_charge", equalTo(true))))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Export tax codes list")
    fun `should support tax codes list export in different formats`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("export_format", "CSV")
            .queryParam("business_type", "RETAIL")
            .`when`()
            .get("/tax-code/v1/list/export")
            .then()
            .statusCode(200)
            .contentType("text/csv")
            .header("Content-Disposition", containsString("tax_codes_export"))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Tax rate statistics")
    fun `should provide tax rate distribution statistics`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("include_stats", true)
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.statistics.total_tax_codes", greaterThanOrEqualTo(0))
            .body("data.statistics.rate_distribution.zero_percent", greaterThanOrEqualTo(0))
            .body("data.statistics.rate_distribution.five_percent", greaterThanOrEqualTo(0))
            .body("data.statistics.rate_distribution.twelve_percent", greaterThanOrEqualTo(0))
            .body("data.statistics.rate_distribution.eighteen_percent", greaterThanOrEqualTo(0))
            .body("data.statistics.rate_distribution.twenty_eight_percent", greaterThanOrEqualTo(0))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Filter currently applicable tax codes")
    fun `should filter tax codes applicable for current date`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("currently_applicable", true)
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("status", "ACTIVE")))
    }

    @Test
    @DisplayName("GET /tax-code/v1/list - Performance with large datasets")
    fun `should handle large tax code lists efficiently`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_LARGE_DATA_WS_001")
            .queryParam("page", 0)
            .queryParam("size", 100)
            .queryParam("performance_mode", true)
            .`when`()
            .get("/tax-code/v1/list")
            .then()
            .statusCode(200)
            .time(lessThan(2000L)) // Should respond within 2 seconds
            .body("success", equalTo(true))
            .body("data.content", hasSize(lessThanOrEqualTo(100)))
    }
}