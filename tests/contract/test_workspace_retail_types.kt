package com.ampairs.tests.contract

import com.ampairs.workspace.model.enums.WorkspaceType
import com.ampairs.workspace.model.dto.CreateWorkspaceRequest
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.workspace.model.dto.WorkspaceResponse
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
 * Contract tests for workspace creation with retail business types.
 * 
 * These tests verify that the workspace creation endpoint properly supports
 * the new retail business types: KIRANA, JEWELRY, HARDWARE
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WorkspaceRetailTypesContractTest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @DisplayName("POST /workspace/v1 - Create KIRANA workspace")
    fun `should create kirana workspace with appropriate features`() {
        val kiranaRequest = CreateWorkspaceRequest(
            name = "Sharma Kirana Store",
            slug = "sharma-kirana",
            workspaceType = WorkspaceType.KIRANA,
            description = "Traditional neighborhood grocery store",
            initialModules = listOf(
                "product-management",
                "customer-management", 
                "order-management"
            )
        )

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token") // TODO: Use test JWT
            .body(kiranaRequest)
            .`when`()
            .post("/workspace/v1")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("data.name", equalTo("Sharma Kirana Store"))
            .body("data.workspace_type", equalTo("KIRANA"))
            .body("data.settings.enabled_modules", hasItems("product-management", "customer-management"))
            // Verify KIRANA-specific features are included
            .body("data.features", hasItems("local_inventory", "credit_management", "neighborhood_delivery"))
            .body("data.member_count", equalTo(0))
            .body("data.created_at", notNullValue())
    }

    @Test
    @DisplayName("POST /workspace/v1 - Create JEWELRY workspace")
    fun `should create jewelry workspace with precious metals features`() {
        val jewelryRequest = CreateWorkspaceRequest(
            name = "Golden Ornaments Ltd",
            slug = "golden-ornaments",
            workspaceType = WorkspaceType.JEWELRY,
            description = "Premium jewelry store with custom designs",
            initialModules = listOf(
                "product-management",
                "inventory-management",
                "customer-management",
                "invoice-generation",
                "tax-management"
            )
        )

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .body(jewelryRequest)
            .`when`()
            .post("/workspace/v1")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("data.name", equalTo("Golden Ornaments Ltd"))
            .body("data.workspace_type", equalTo("JEWELRY"))
            // Verify JEWELRY-specific features are included
            .body("data.features", hasItems("precious_metals", "custom_designs", "certification_tracking", "weight_based_pricing"))
            .body("data.settings.currency", equalTo("INR"))
            .body("data.settings.timezone", equalTo("Asia/Kolkata"))
    }

    @Test
    @DisplayName("POST /workspace/v1 - Create HARDWARE workspace")
    fun `should create hardware workspace with construction supply features`() {
        val hardwareRequest = CreateWorkspaceRequest(
            name = "ABC Hardware & Construction",
            slug = "abc-hardware",
            workspaceType = WorkspaceType.HARDWARE,
            description = "Complete hardware and construction supplies",
            initialModules = listOf(
                "product-management",
                "inventory-management",
                "customer-management",
                "order-management",
                "invoice-generation"
            )
        )

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .body(hardwareRequest)
            .`when`()
            .post("/workspace/v1")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("data.name", equalTo("ABC Hardware & Construction"))
            .body("data.workspace_type", equalTo("HARDWARE"))
            // Verify HARDWARE-specific features are included
            .body("data.features", hasItems("bulk_inventory", "construction_supplies", "contractor_accounts", "delivery_logistics"))
            .body("data.max_users", equalTo(30)) // Hardware stores support up to 30 users
    }

    @Test
    @DisplayName("POST /workspace/v1 - Validation error for invalid retail type")
    fun `should return validation error for unsupported workspace type`() {
        val invalidRequest = """
            {
                "name": "Test Store",
                "slug": "test-store", 
                "workspace_type": "INVALID_RETAIL_TYPE",
                "description": "Should fail validation"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .body(invalidRequest)
            .`when`()
            .post("/workspace/v1")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.message", containsString("workspace_type"))
            .body("error.module", equalTo("workspace"))
    }

    @Test
    @DisplayName("POST /workspace/v1 - Module availability for retail types")
    fun `should validate module availability for different retail types`() {
        // Test that retail-specific modules are available for KIRANA workspace
        val kiranaRequest = CreateWorkspaceRequest(
            name = "Test Kirana",
            slug = "test-kirana",
            workspaceType = WorkspaceType.KIRANA,
            initialModules = listOf(
                "product-management",
                "inventory-management", // Should be available
                "smart-notifications"   // Should be available
            )
        )

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .body(kiranaRequest)
            .`when`()
            .post("/workspace/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.settings.enabled_modules", hasSize(greaterThan(0)))
    }

    @Test
    @DisplayName("GET /workspace/v1/{workspaceId} - Retrieve retail workspace details")
    fun `should retrieve retail workspace with all retail-specific attributes`() {
        // This test assumes a JEWELRY workspace exists with ID "TEST_JEWELRY_WS_001"
        // In actual implementation, this would be created in a @BeforeEach setup
        
        RestAssured
            .given()
            .header("Authorization", "Bearer valid_jwt_token")
            .`when`()
            .get("/workspace/v1/TEST_JEWELRY_WS_001")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.workspace_type", equalTo("JEWELRY"))
            .body("data.features", hasItems("precious_metals", "custom_designs"))
            .body("data.settings.enabled_modules", notNullValue())
            .body("data.member_count", greaterThanOrEqualTo(0))
    }
}

/**
 * Data class for workspace creation request (matches API contract)
 * This should match the actual DTO in the workspace module
 */
data class CreateWorkspaceRequest(
    val name: String,
    val slug: String,
    val workspaceType: WorkspaceType,
    val description: String? = null,
    val initialModules: List<String> = emptyList()
)