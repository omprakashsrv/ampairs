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
 * Contract tests for workspace listing with retail module filtering.
 * 
 * These tests verify that workspace listing endpoints properly filter
 * by retail modules and return appropriate module information.
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WorkspaceRetailModulesContractTest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @DisplayName("GET /workspace/v1/list - Filter workspaces by retail modules")
    fun `should filter workspaces by enabled retail modules`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .queryParam("page", 0)
            .queryParam("size", 20)
            .queryParam("module_filter", "product-management")
            .`when`()
            .get("/workspace/v1/list")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("data.content", notNullValue())
            .body("data.page", equalTo(0))
            .body("data.size", equalTo(20))
            .body("data.total_elements", greaterThanOrEqualTo(0))
            .body("data.content[0].enabled_modules", hasItem("product-management"))
    }

    @Test
    @DisplayName("GET /workspace/v1/list - Filter by multiple retail modules")
    fun `should filter workspaces by multiple enabled modules`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .queryParam("module_filter", "product-management,inventory-management")
            .`when`()
            .get("/workspace/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content[0].enabled_modules", hasItems("product-management", "inventory-management"))
    }

    @Test
    @DisplayName("GET /workspace/v1/{workspaceId}/modules - Get available retail modules")
    fun `should return available retail modules for workspace`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .`when`()
            .get("/workspace/v1/TEST_RETAIL_WS_001/modules")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.available_modules", hasItems(
                hasEntry("code", "product-management"),
                hasEntry("code", "inventory-management"), 
                hasEntry("code", "order-management"),
                hasEntry("code", "customer-management"),
                hasEntry("code", "invoice-generation"),
                hasEntry("code", "tax-management"),
                hasEntry("code", "retail-analytics"),
                hasEntry("code", "smart-notifications")
            ))
    }

    @Test
    @DisplayName("GET /workspace/v1/{workspaceId}/modules - Module details include retail-specific metadata")
    fun `should return retail module details with business relevance`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .`when`()
            .get("/workspace/v1/TEST_JEWELRY_WS_001/modules")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            // Verify product-management module has high relevance for JEWELRY
            .body("data.available_modules.find { it.code == 'product-management' }.business_relevance.find { it.business_type == 'JEWELRY' }.relevance_score", 
                  equalTo(10))
            .body("data.available_modules.find { it.code == 'product-management' }.business_relevance.find { it.business_type == 'JEWELRY' }.is_essential", 
                  equalTo(true))
    }

    @Test 
    @DisplayName("POST /workspace/v1/{workspaceId}/modules/install - Install retail module")
    fun `should install retail module to workspace`() {
        val installRequest = """
            {
                "module_code": "inventory-management",
                "configuration": {
                    "enable_low_stock_alerts": true,
                    "enable_movement_tracking": true,
                    "default_reorder_level": 10
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .body(installRequest)
            .`when`()
            .post("/workspace/v1/TEST_RETAIL_WS_001/modules/install")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.module_code", equalTo("inventory-management"))
            .body("data.status", equalTo("INSTALLED"))
            .body("data.installed_at", notNullValue())
    }

    @Test
    @DisplayName("DELETE /workspace/v1/{workspaceId}/modules/{moduleCode} - Uninstall retail module")
    fun `should uninstall retail module from workspace`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .`when`()
            .delete("/workspace/v1/TEST_RETAIL_WS_001/modules/smart-notifications")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.message", containsString("uninstalled successfully"))
    }

    @Test
    @DisplayName("GET /workspace/v1/{workspaceId}/modules/installed - List installed retail modules")
    fun `should return list of installed retail modules`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .`when`()
            .get("/workspace/v1/TEST_RETAIL_WS_001/modules/installed")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data", hasSize(greaterThan(0)))
            .body("data[0].module_code", notNullValue())
            .body("data[0].status", equalTo("ACTIVE"))
            .body("data[0].installed_at", notNullValue())
    }

    @Test
    @DisplayName("PUT /workspace/v1/{workspaceId}/modules/{moduleCode}/configure - Configure retail module")
    fun `should configure retail module settings`() {
        val configRequest = """
            {
                "configuration": {
                    "enable_gst_compliance": true,
                    "default_tax_rate": 18.0,
                    "invoice_template": "retail_standard",
                    "auto_generate_invoice_number": true
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .body(configRequest)
            .`when`()
            .put("/workspace/v1/TEST_RETAIL_WS_001/modules/invoice-generation/configure")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.configuration.enable_gst_compliance", equalTo(true))
            .body("data.configuration.default_tax_rate", equalTo(18.0f))
    }

    @Test
    @DisplayName("GET /workspace/v1/modules/search - Search retail modules by category")
    fun `should search retail modules by category and business type`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .queryParam("category", "COMMERCE")
            .queryParam("business_type", "RETAIL")
            .queryParam("search", "product")
            .`when`()
            .get("/workspace/v1/modules/search")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThan(0)))
            .body("data.content[0].category", equalTo("COMMERCE"))
            .body("data.content[0].name", containsStringIgnoringCase("product"))
    }

    @Test
    @DisplayName("GET /workspace/v1/modules/recommendations - Get module recommendations for retail workspace")
    fun `should return module recommendations based on business type`() {
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .queryParam("workspace_id", "TEST_KIRANA_WS_001")
            .`when`()
            .get("/workspace/v1/modules/recommendations")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.essential_modules", hasItems(
                hasEntry("code", "product-management"),
                hasEntry("code", "customer-management")
            ))
            .body("data.recommended_modules", notNullValue())
            .body("data.business_type", equalTo("KIRANA"))
    }
}