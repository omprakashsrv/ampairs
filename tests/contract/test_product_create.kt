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
 * Contract tests for Product Management API - Create Product endpoint.
 * 
 * Tests verify the POST /product/v1 endpoint according to the retail API contract.
 * Covers product creation with various retail business scenarios.
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductCreateContractTest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @DisplayName("POST /product/v1 - Create basic retail product")
    fun `should create basic retail product with required fields`() {
        val productRequest = """
            {
                "name": "Steel Hammer 500g",
                "sku": "HAM-ST-500",
                "description": "Heavy duty steel hammer for construction",
                "unit_id": "unit-pieces",
                "tax_code_id": "tax-gst-18",
                "base_price": 450.00,
                "cost_price": 300.00
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_HARDWARE_WS_001")
            .body(productRequest)
            .`when`()
            .post("/product/v1")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("data.id", notNullValue())
            .body("data.sku", equalTo("HAM-ST-500"))
            .body("data.name", equalTo("Steel Hammer 500g"))
            .body("data.base_price", equalTo(450.0f))
            .body("data.cost_price", equalTo(300.0f))
            .body("data.status", equalTo("ACTIVE"))
            .body("data.created_at", notNullValue())
            .body("data.updated_at", notNullValue())
    }

    @Test
    @DisplayName("POST /product/v1 - Create jewelry product with weight attributes")
    fun `should create jewelry product with precious metal attributes`() {
        val jewelryProductRequest = """
            {
                "name": "Gold Ring 22K",
                "sku": "RING-GOLD-22K-001",
                "description": "22 karat gold ring with diamond setting",
                "unit_id": "unit-grams",
                "tax_code_id": "tax-gst-3",
                "base_price": 8500.00,
                "cost_price": 7200.00,
                "attributes": {
                    "weight_grams": 5.2,
                    "purity": "22K",
                    "metal_type": "GOLD",
                    "stone_type": "DIAMOND",
                    "stone_weight_carats": 0.25,
                    "certification": "BIS_HALLMARK"
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
            .body(jewelryProductRequest)
            .`when`()
            .post("/product/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.name", equalTo("Gold Ring 22K"))
            .body("data.attributes.weight_grams", equalTo(5.2f))
            .body("data.attributes.purity", equalTo("22K"))
            .body("data.attributes.metal_type", equalTo("GOLD"))
            .body("data.attributes.certification", equalTo("BIS_HALLMARK"))
    }

    @Test
    @DisplayName("POST /product/v1 - Create kirana product with bulk pricing")
    fun `should create kirana product with local inventory features`() {
        val kiranaProductRequest = """
            {
                "name": "Basmati Rice Premium",
                "sku": "RICE-BAS-PREM-10KG",
                "description": "Premium quality basmati rice 10kg pack",
                "unit_id": "unit-kilograms", 
                "tax_code_id": "tax-gst-5",
                "base_price": 850.00,
                "cost_price": 720.00,
                "attributes": {
                    "pack_size": "10KG",
                    "brand": "India Gate",
                    "expiry_tracking": true,
                    "minimum_order_quantity": 1,
                    "bulk_discount_threshold": 5
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
            .body(kiranaProductRequest)
            .`when`()
            .post("/product/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.name", equalTo("Basmati Rice Premium"))
            .body("data.attributes.pack_size", equalTo("10KG"))
            .body("data.attributes.expiry_tracking", equalTo(true))
    }

    @Test
    @DisplayName("POST /product/v1 - Validation error for missing required fields")
    fun `should return validation error when required fields are missing`() {
        val invalidProductRequest = """
            {
                "name": "Incomplete Product",
                "sku": "INCOMPLETE-001"
                // Missing unit_id, tax_code_id, base_price
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidProductRequest)
            .`when`()
            .post("/product/v1")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.validation_errors.unit_id", containsString("required"))
            .body("error.validation_errors.tax_code_id", containsString("required"))
            .body("error.validation_errors.base_price", containsString("required"))
    }

    @Test
    @DisplayName("POST /product/v1 - Duplicate SKU validation")
    fun `should return conflict error when SKU already exists in workspace`() {
        val duplicateSkuRequest = """
            {
                "name": "Duplicate SKU Product",
                "sku": "EXISTING-SKU-001",
                "unit_id": "unit-pieces",
                "tax_code_id": "tax-gst-18", 
                "base_price": 100.00
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(duplicateSkuRequest)
            .`when`()
            .post("/product/v1")
            .then()
            .statusCode(409)
            .body("success", equalTo(false))
            .body("error.code", equalTo("DUPLICATE_ENTRY"))
            .body("error.message", containsString("SKU"))
            .body("error.details", containsString("EXISTING-SKU-001"))
    }

    @Test
    @DisplayName("POST /product/v1 - Multi-tenant isolation validation")
    fun `should allow same SKU in different workspaces`() {
        val sameSkuRequest = """
            {
                "name": "Same SKU Different Workspace",
                "sku": "SHARED-SKU-001",
                "unit_id": "unit-pieces",
                "tax_code_id": "tax-gst-18",
                "base_price": 200.00
            }
        """.trimIndent()

        // Create in first workspace
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_A")
            .body(sameSkuRequest)
            .`when`()
            .post("/product/v1")
            .then()
            .statusCode(201)

        // Create same SKU in second workspace - should succeed
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_B")
            .body(sameSkuRequest)
            .`when`()
            .post("/product/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
    }

    @Test
    @DisplayName("POST /product/v1 - Product with images")
    fun `should create product with multiple images`() {
        val productWithImagesRequest = """
            {
                "name": "Premium Leather Sofa",
                "sku": "SOFA-LEATHER-001",
                "description": "3-seater premium leather sofa",
                "unit_id": "unit-pieces",
                "tax_code_id": "tax-gst-28",
                "base_price": 45000.00,
                "cost_price": 35000.00,
                "images": [
                    {
                        "url": "https://example.com/sofa-front.jpg",
                        "alt": "Front view of leather sofa",
                        "is_primary": true,
                        "sort_order": 0
                    },
                    {
                        "url": "https://example.com/sofa-side.jpg", 
                        "alt": "Side view of leather sofa",
                        "is_primary": false,
                        "sort_order": 1
                    }
                ]
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(productWithImagesRequest)
            .`when`()
            .post("/product/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.images", hasSize(2))
            .body("data.images[0].is_primary", equalTo(true))
            .body("data.images[1].is_primary", equalTo(false))
    }

    @Test
    @DisplayName("POST /product/v1 - Unauthorized access without workspace header")
    fun `should return unauthorized when workspace header is missing`() {
        val productRequest = """
            {
                "name": "Test Product",
                "sku": "TEST-001",
                "unit_id": "unit-pieces",
                "tax_code_id": "tax-gst-18",
                "base_price": 100.00
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            // Missing X-Workspace-ID header
            .body(productRequest)
            .`when`()
            .post("/product/v1")
            .then()
            .statusCode(400)
            .body("error.code", equalTo("INVALID_TENANT_CONTEXT"))
    }
}