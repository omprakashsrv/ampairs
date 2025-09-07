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
 * Contract tests for Tax Code Management API - Create Tax Code endpoint.
 * 
 * Tests verify the POST /tax-code/v1 endpoint according to the retail API contract.
 * Covers GST tax code creation with various rates and business type applicability.
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TaxCodeCreateContractTest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @DisplayName("POST /tax-code/v1 - Create GST tax code with 18% rate")
    fun `should create GST tax code with standard 18 percent rate`() {
        val taxCodeRequest = """
            {
                "code": "GST-18-ELECTRONICS",
                "name": "GST 18% - Electronics",
                "description": "18% GST applicable on electronic goods",
                "tax_type": "GST",
                "rate": 18.0,
                "hsn_code": "8471",
                "applicable_business_types": ["RETAIL", "KIRANA", "HARDWARE"],
                "effective_from": "2025-09-07",
                "is_compound": false
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(taxCodeRequest)
            .`when`()
            .post("/tax-code/v1")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("data.id", notNullValue())
            .body("data.code", equalTo("GST-18-ELECTRONICS"))
            .body("data.name", equalTo("GST 18% - Electronics"))
            .body("data.tax_type", equalTo("GST"))
            .body("data.rate", equalTo(18.0f))
            .body("data.hsn_code", equalTo("8471"))
            .body("data.status", equalTo("ACTIVE"))
            .body("data.created_at", notNullValue())
            .body("data.effective_from", equalTo("2025-09-07"))
    }

    @Test
    @DisplayName("POST /tax-code/v1 - Create GST tax code with 5% rate for essentials")
    fun `should create GST tax code with 5 percent rate for essential goods`() {
        val taxCodeRequest = """
            {
                "code": "GST-5-FOOD",
                "name": "GST 5% - Food Items",
                "description": "5% GST applicable on essential food items",
                "tax_type": "GST",
                "rate": 5.0,
                "hsn_code": "1006",
                "applicable_business_types": ["KIRANA", "RETAIL"],
                "effective_from": "2025-09-07",
                "is_compound": false,
                "exemption_threshold": 40000000.0
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
            .body(taxCodeRequest)
            .`when`()
            .post("/tax-code/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.code", equalTo("GST-5-FOOD"))
            .body("data.rate", equalTo(5.0f))
            .body("data.hsn_code", equalTo("1006"))
            .body("data.applicable_business_types", hasItems("KIRANA", "RETAIL"))
            .body("data.exemption_threshold", equalTo(40000000.0f))
    }

    @Test
    @DisplayName("POST /tax-code/v1 - Create GST tax code with 3% rate for precious metals")
    fun `should create GST tax code with 3 percent rate for jewelry`() {
        val taxCodeRequest = """
            {
                "code": "GST-3-JEWELRY",
                "name": "GST 3% - Precious Metals",
                "description": "3% GST applicable on gold, silver and precious metals",
                "tax_type": "GST",
                "rate": 3.0,
                "hsn_code": "7113",
                "applicable_business_types": ["JEWELRY"],
                "effective_from": "2025-09-07",
                "is_compound": false,
                "special_provisions": {
                    "reverse_charge": false,
                    "tax_on_making_charges": true,
                    "wastage_taxable": false
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
            .body(taxCodeRequest)
            .`when`()
            .post("/tax-code/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.code", equalTo("GST-3-JEWELRY"))
            .body("data.rate", equalTo(3.0f))
            .body("data.applicable_business_types", hasItem("JEWELRY"))
            .body("data.special_provisions.tax_on_making_charges", equalTo(true))
            .body("data.special_provisions.wastage_taxable", equalTo(false))
    }

    @Test
    @DisplayName("POST /tax-code/v1 - Create GST tax code with 28% rate for luxury items")
    fun `should create GST tax code with 28 percent rate for luxury goods`() {
        val taxCodeRequest = """
            {
                "code": "GST-28-LUXURY",
                "name": "GST 28% - Luxury Goods",
                "description": "28% GST applicable on luxury and sin goods",
                "tax_type": "GST",
                "rate": 28.0,
                "hsn_code": "9403",
                "applicable_business_types": ["RETAIL", "HARDWARE"],
                "effective_from": "2025-09-07",
                "is_compound": false,
                "cess_applicable": true,
                "cess_rate": 5.0
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(taxCodeRequest)
            .`when`()
            .post("/tax-code/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.code", equalTo("GST-28-LUXURY"))
            .body("data.rate", equalTo(28.0f))
            .body("data.cess_applicable", equalTo(true))
            .body("data.cess_rate", equalTo(5.0f))
            .body("data.effective_tax_rate", equalTo(33.0f)) // 28% + 5% cess
    }

    @Test
    @DisplayName("POST /tax-code/v1 - Create exempt tax code for non-taxable items")
    fun `should create exempt tax code for zero-rated items`() {
        val exemptTaxCodeRequest = """
            {
                "code": "GST-EXEMPT-BASIC",
                "name": "GST Exempt - Basic Necessities",
                "description": "Tax exempt items like milk, bread, vegetables",
                "tax_type": "GST",
                "rate": 0.0,
                "hsn_code": "0401",
                "applicable_business_types": ["KIRANA", "RETAIL"],
                "effective_from": "2025-09-07",
                "is_exempt": true,
                "exemption_reason": "Essential commodities for public welfare"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
            .body(exemptTaxCodeRequest)
            .`when`()
            .post("/tax-code/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.code", equalTo("GST-EXEMPT-BASIC"))
            .body("data.rate", equalTo(0.0f))
            .body("data.is_exempt", equalTo(true))
            .body("data.exemption_reason", equalTo("Essential commodities for public welfare"))
    }

    @Test
    @DisplayName("POST /tax-code/v1 - Create composite tax code with multiple components")
    fun `should create composite tax code with CGST and SGST breakdown`() {
        val compositeTaxCodeRequest = """
            {
                "code": "GST-12-INTRASTATE",
                "name": "GST 12% - Intra-state Supply",
                "description": "12% GST split as 6% CGST + 6% SGST for intra-state transactions",
                "tax_type": "GST",
                "rate": 12.0,
                "hsn_code": "3004",
                "applicable_business_types": ["RETAIL", "KIRANA"],
                "effective_from": "2025-09-07",
                "is_compound": true,
                "components": [
                    {
                        "component_type": "CGST",
                        "rate": 6.0,
                        "account_code": "CGST_PAYABLE"
                    },
                    {
                        "component_type": "SGST", 
                        "rate": 6.0,
                        "account_code": "SGST_PAYABLE"
                    }
                ]
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(compositeTaxCodeRequest)
            .`when`()
            .post("/tax-code/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.code", equalTo("GST-12-INTRASTATE"))
            .body("data.is_compound", equalTo(true))
            .body("data.components", hasSize(2))
            .body("data.components[0].component_type", equalTo("CGST"))
            .body("data.components[0].rate", equalTo(6.0f))
            .body("data.components[1].component_type", equalTo("SGST"))
            .body("data.components[1].rate", equalTo(6.0f))
    }

    @Test
    @DisplayName("POST /tax-code/v1 - Validation error for missing required fields")
    fun `should return validation error when required fields are missing`() {
        val invalidTaxCodeRequest = """
            {
                "name": "Incomplete Tax Code",
                "tax_type": "GST"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidTaxCodeRequest)
            .`when`()
            .post("/tax-code/v1")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.validation_errors.code", containsString("required"))
            .body("error.validation_errors.rate", containsString("required"))
            .body("error.validation_errors.hsn_code", containsString("required"))
    }

    @Test
    @DisplayName("POST /tax-code/v1 - Duplicate tax code validation")
    fun `should return conflict error when tax code already exists in workspace`() {
        val duplicateCodeRequest = """
            {
                "code": "EXISTING-TAX-CODE-001",
                "name": "Duplicate Tax Code",
                "tax_type": "GST",
                "rate": 18.0,
                "hsn_code": "9999",
                "applicable_business_types": ["RETAIL"],
                "effective_from": "2025-09-07"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(duplicateCodeRequest)
            .`when`()
            .post("/tax-code/v1")
            .then()
            .statusCode(409)
            .body("success", equalTo(false))
            .body("error.code", equalTo("DUPLICATE_ENTRY"))
            .body("error.message", containsString("tax code"))
            .body("error.details", containsString("EXISTING-TAX-CODE-001"))
    }

    @Test
    @DisplayName("POST /tax-code/v1 - Invalid tax rate validation")
    fun `should validate tax rate within acceptable range`() {
        val invalidRateRequest = """
            {
                "code": "INVALID-RATE-CODE",
                "name": "Invalid Rate Tax Code",
                "tax_type": "GST",
                "rate": 150.0,
                "hsn_code": "9999",
                "applicable_business_types": ["RETAIL"],
                "effective_from": "2025-09-07"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidRateRequest)
            .`when`()
            .post("/tax-code/v1")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.validation_errors.rate", containsString("must be between 0 and 100"))
    }

    @Test
    @DisplayName("POST /tax-code/v1 - Invalid HSN code format validation")
    fun `should validate HSN code format`() {
        val invalidHsnRequest = """
            {
                "code": "VALID-CODE-001",
                "name": "Tax Code with Invalid HSN",
                "tax_type": "GST",
                "rate": 18.0,
                "hsn_code": "INVALID-HSN",
                "applicable_business_types": ["RETAIL"],
                "effective_from": "2025-09-07"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidHsnRequest)
            .`when`()
            .post("/tax-code/v1")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.validation_errors.hsn_code", containsString("invalid format"))
    }

    @Test
    @DisplayName("POST /tax-code/v1 - Multi-tenant isolation validation")
    fun `should allow same tax code in different workspaces`() {
        val sameCodeRequest = """
            {
                "code": "SHARED-TAX-CODE-001",
                "name": "Same Code Different Workspace",
                "tax_type": "GST",
                "rate": 18.0,
                "hsn_code": "8888",
                "applicable_business_types": ["RETAIL"],
                "effective_from": "2025-09-07"
            }
        """.trimIndent()

        // Create in first workspace
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_A")
            .body(sameCodeRequest)
            .`when`()
            .post("/tax-code/v1")
            .then()
            .statusCode(201)

        // Create same code in second workspace - should succeed
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_B")
            .body(sameCodeRequest)
            .`when`()
            .post("/tax-code/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
    }

    @Test
    @DisplayName("POST /tax-code/v1 - Future effective date validation")
    fun `should allow tax codes with future effective dates`() {
        val futureEffectiveRequest = """
            {
                "code": "FUTURE-TAX-CODE-001",
                "name": "Future Effective Tax Code",
                "tax_type": "GST",
                "rate": 15.0,
                "hsn_code": "7777",
                "applicable_business_types": ["RETAIL"],
                "effective_from": "2026-01-01",
                "effective_to": "2026-12-31"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(futureEffectiveRequest)
            .`when`()
            .post("/tax-code/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.status", equalTo("SCHEDULED"))
            .body("data.effective_from", equalTo("2026-01-01"))
            .body("data.effective_to", equalTo("2026-12-31"))
    }

    @Test
    @DisplayName("POST /tax-code/v1 - Business type applicability validation")
    fun `should validate business type applicability`() {
        val invalidBusinessTypeRequest = """
            {
                "code": "INVALID-BUSINESS-TYPE-CODE",
                "name": "Invalid Business Type Code",
                "tax_type": "GST",
                "rate": 18.0,
                "hsn_code": "6666",
                "applicable_business_types": ["INVALID_TYPE"],
                "effective_from": "2025-09-07"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidBusinessTypeRequest)
            .`when`()
            .post("/tax-code/v1")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.validation_errors.applicable_business_types", 
                  containsString("must be one of: RETAIL, KIRANA, JEWELRY, HARDWARE"))
    }

    @Test
    @DisplayName("POST /tax-code/v1 - Tax code creation audit logging")
    fun `should track user who created the tax code`() {
        val taxCodeRequest = """
            {
                "code": "AUDIT-TAX-CODE-001",
                "name": "Audit Test Tax Code",
                "tax_type": "GST",
                "rate": 12.0,
                "hsn_code": "5555",
                "applicable_business_types": ["RETAIL"],
                "effective_from": "2025-09-07"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(taxCodeRequest)
            .`when`()
            .post("/tax-code/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.created_by.id", notNullValue())
            .body("data.created_by.name", notNullValue())
            .body("data.created_at", notNullValue())
            .body("data.updated_at", notNullValue())
    }

    @Test
    @DisplayName("POST /tax-code/v1 - Reverse charge mechanism support")
    fun `should create tax code with reverse charge mechanism`() {
        val reverseChargeRequest = """
            {
                "code": "GST-18-REVERSE-CHARGE",
                "name": "GST 18% - Reverse Charge",
                "description": "18% GST with reverse charge mechanism for B2B services",
                "tax_type": "GST",
                "rate": 18.0,
                "hsn_code": "9954",
                "applicable_business_types": ["RETAIL", "HARDWARE"],
                "effective_from": "2025-09-07",
                "special_provisions": {
                    "reverse_charge": true,
                    "reverse_charge_threshold": 250000.0,
                    "supplier_pays": false,
                    "recipient_pays": true
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(reverseChargeRequest)
            .`when`()
            .post("/tax-code/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.special_provisions.reverse_charge", equalTo(true))
            .body("data.special_provisions.reverse_charge_threshold", equalTo(250000.0f))
            .body("data.special_provisions.recipient_pays", equalTo(true))
    }
}