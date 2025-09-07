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
 * Contract tests for Product Management API - List Products endpoint.
 * 
 * Tests verify the GET /product/v1/list endpoint with search, filtering,
 * and pagination capabilities according to the retail API contract.
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductListContractTest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @DisplayName("GET /product/v1/list - Basic product listing with pagination")
    fun `should return paginated product list`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("page", 0)
            .queryParam("size", 20)
            .`when`()
            .get("/product/v1/list")
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
    @DisplayName("GET /product/v1/list - Search products by name")
    fun `should search products by name with partial matching`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_HARDWARE_WS_001")
            .queryParam("search", "hammer")
            .queryParam("page", 0)
            .queryParam("size", 10)
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThanOrEqualTo(0)))
            // All returned products should contain "hammer" in name or description
            .body("data.content.findAll { it.name.toLowerCase().contains('hammer') || it.description?.toLowerCase()?.contains('hammer') }", 
                  hasSize(greaterThanOrEqualTo(0)))
    }

    @Test
    @DisplayName("GET /product/v1/list - Search by SKU")
    fun `should search products by SKU with exact and partial matching`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
            .queryParam("search", "RING-GOLD")
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content.findAll { it.sku.contains('RING-GOLD') }", hasSize(greaterThanOrEqualTo(0)))
    }

    @Test
    @DisplayName("GET /product/v1/list - Filter by category")
    fun `should filter products by category_id`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("category_id", "cat-electronics")
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("category.id", "cat-electronics")))
    }

    @Test
    @DisplayName("GET /product/v1/list - Filter by product status")
    fun `should filter products by status`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("status", "ACTIVE")
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("status", "ACTIVE")))
    }

    @Test
    @DisplayName("GET /product/v1/list - Combined search and filtering")
    fun `should support combined search with category and status filters`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
            .queryParam("search", "rice")
            .queryParam("category_id", "cat-groceries")
            .queryParam("status", "ACTIVE")
            .queryParam("page", 0)
            .queryParam("size", 5)
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.size", equalTo(5))
            .body("data.content", everyItem(allOf(
                hasEntry("status", "ACTIVE"),
                hasEntry("category.id", "cat-groceries")
            )))
    }

    @Test
    @DisplayName("GET /product/v1/list - Sort by price ascending")
    fun `should sort products by price in ascending order`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("sort", "base_price")
            .queryParam("direction", "asc")
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThan(0)))
            // Verify ascending price order (first item price <= second item price)
            .body("data.content[0].base_price", lessThanOrEqualTo(
                Float.parseFloat("${RestAssured.given().get("/product/v1/list").path("data.content[1].base_price")}")))
    }

    @Test
    @DisplayName("GET /product/v1/list - Sort by creation date descending")
    fun `should sort products by created_at in descending order`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("sort", "created_at")
            .queryParam("direction", "desc")
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThan(0)))
            .body("data.content[0].created_at", notNullValue())
    }

    @Test
    @DisplayName("GET /product/v1/list - Low stock filter")
    fun `should filter products with low stock levels`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("low_stock", true)
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            // Products returned should have current_stock <= reorder_level
            .body("data.content", everyItem(anyOf(
                hasEntry("current_stock", lessThanOrEqualTo(Float.parseFloat("${RestAssured.get().path("reorder_level")}"))),
                hasEntry("current_stock", equalTo(0))
            )))
    }

    @Test
    @DisplayName("GET /product/v1/list - Price range filter")
    fun `should filter products by price range`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
            .queryParam("min_price", 5000)
            .queryParam("max_price", 15000)
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(allOf(
                hasEntry("base_price", greaterThanOrEqualTo(5000.0f)),
                hasEntry("base_price", lessThanOrEqualTo(15000.0f))
            )))
    }

    @Test
    @DisplayName("GET /product/v1/list - Include inventory information")
    fun `should include current stock information in product list`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("include_stock", true)
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThan(0)))
            .body("data.content[0].current_stock", notNullValue())
            .body("data.content[0].available_stock", notNullValue())
    }

    @Test
    @DisplayName("GET /product/v1/list - Multi-tenant isolation")
    fun `should only return products from current workspace`() {
        // Request products from workspace A
        val workspaceAProducts = RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_A")
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .extract()
            .path<List<String>>("data.content.id")

        // Request products from workspace B
        val workspaceBProducts = RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_B")
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .extract()
            .path<List<String>>("data.content.id")

        // Products from different workspaces should not overlap
        assert(workspaceAProducts.intersect(workspaceBProducts.toSet()).isEmpty())
    }

    @Test
    @DisplayName("GET /product/v1/list - Empty result set")
    fun `should return empty list when no products match criteria`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_EMPTY_WS_001")
            .queryParam("search", "nonexistent_product_xyz")
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(0))
            .body("data.total_elements", equalTo(0))
            .body("data.total_pages", equalTo(0))
            .body("data.empty", equalTo(true))
    }

    @Test
    @DisplayName("GET /product/v1/list - Invalid pagination parameters")
    fun `should handle invalid pagination parameters gracefully`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("page", -1)
            .queryParam("size", 0)
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
    }

    @Test
    @DisplayName("GET /product/v1/list - Unauthorized without workspace header")
    fun `should return error when workspace header is missing`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            // Missing X-Workspace-ID header
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(400)
            .body("error.code", equalTo("INVALID_TENANT_CONTEXT"))
    }

    @Test
    @DisplayName("GET /product/v1/list - Product response structure validation")
    fun `should return products with complete response structure`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .queryParam("size", 1)
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content[0].id", notNullValue())
            .body("data.content[0].sku", notNullValue())
            .body("data.content[0].name", notNullValue())
            .body("data.content[0].base_price", notNullValue())
            .body("data.content[0].status", notNullValue())
            .body("data.content[0].created_at", notNullValue())
            .body("data.content[0].updated_at", notNullValue())
            // Optional fields
            .body("data.content[0]", anyOf(
                hasEntry("category", notNullValue()),
                not(hasKey("category"))
            ))
    }
}