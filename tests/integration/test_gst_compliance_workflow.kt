package com.ampairs.tests.integration

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Integration tests for complete GST compliance workflow.
 * 
 * Tests the end-to-end process of GST tax code management, tax calculations,
 * GST return preparation, and compliance reporting across different business types.
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class GSTComplianceWorkflowIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    private var gstRegisteredWorkspaceId: String = ""
    private var kiranaWorkspaceId: String = ""
    private var jewelryWorkspaceId: String = ""
    private var authToken: String = "valid_jwt_token"
    private val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("MM"))
    private val currentYear = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"))

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @Order(1)
    @DisplayName("Setup: Create GST-registered businesses for compliance testing")
    fun `should create GST-registered businesses with complete tax setup`() {
        // Create main GST-registered workspace
        val gstWorkspaceRequest = """
            {
                "name": "Bharat Enterprises Pvt Ltd",
                "description": "Multi-category retail business with full GST compliance",
                "business_type": "RETAIL",
                "owner_details": {
                    "name": "Manoj Agarwal",
                    "phone": "+919876543280",
                    "email": "manoj@bharatenterprises.com"
                },
                "business_details": {
                    "gstin": "09AAACB1234C1Z5",
                    "pan": "AAACB1234C",
                    "state_code": "09",
                    "business_address": {
                        "street": "Industrial Area Phase 1",
                        "city": "Delhi",
                        "state": "Delhi", 
                        "postal_code": "110020",
                        "country": "India"
                    },
                    "gst_registration_date": "2017-07-01",
                    "turnover_previous_year": 12500000.00,
                    "gst_category": "Regular"
                }
            }
        """.trimIndent()

        val gstResponse = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .body(gstWorkspaceRequest)
            .`when`()
            .post("/workspace/v1")
            .then()
            .statusCode(201)
            .body("data.business_details.gstin", equalTo("09AAACB1234C1Z5"))
            .extract()

        gstRegisteredWorkspaceId = gstResponse.path("data.id")

        // Create Kirana workspace for small business GST
        val kiranaWorkspaceRequest = """
            {
                "name": "Shree Ram Kirana Store",
                "description": "Neighborhood kirana store with composition scheme",
                "business_type": "KIRANA",
                "owner_details": {
                    "name": "Raman Sharma",
                    "phone": "+919876543281",
                    "email": "raman@shreerankirana.com"
                },
                "business_details": {
                    "gstin": "27DEFGH5678I1J2",
                    "pan": "DEFGH5678I",
                    "state_code": "27",
                    "gst_category": "Composition",
                    "composition_rate": 1.0
                }
            }
        """.trimIndent()

        val kiranaResponse = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .body(kiranaWorkspaceRequest)
            .`when`()
            .post("/workspace/v1")
            .then()
            .statusCode(201)
            .extract()

        kiranaWorkspaceId = kiranaResponse.path("data.id")

        // Create Jewelry workspace for precious metals GST
        val jewelryWorkspaceRequest = """
            {
                "name": "Maharaja Gold Palace",
                "description": "Premium jewelry showroom with precious metals",
                "business_type": "JEWELRY",
                "owner_details": {
                    "name": "Suresh Jewelers",
                    "phone": "+919876543282",
                    "email": "suresh@maharajagold.com"
                },
                "business_details": {
                    "gstin": "36KLMNO9876P5Q4",
                    "pan": "KLMNO9876P",
                    "state_code": "36",
                    "special_category": "Precious_Metals_Dealer",
                    "hallmark_license": "H-2024-KAR-001234"
                }
            }
        """.trimIndent()

        val jewelryResponse = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .body(jewelryWorkspaceRequest)
            .`when`()
            .post("/workspace/v1")
            .then()
            .statusCode(201)
            .extract()

        jewelryWorkspaceId = jewelryResponse.path("data.id")

        // Setup comprehensive tax codes for each business
        setupComprehensiveTaxCodes()
    }

    private fun setupComprehensiveTaxCodes() {
        // Setup standard GST tax codes for main business
        val standardGstCodes = listOf(
            // Exempt items
            """
            {
                "code": "GST-EXEMPT-ESSENTIALS",
                "name": "GST Exempt - Essential Items",
                "tax_type": "GST",
                "rate": 0.0,
                "hsn_code": "1006",
                "applicable_business_types": ["RETAIL", "KIRANA"],
                "effective_from": "2017-07-01",
                "is_exempt": true
            }
            """,
            // 5% GST items
            """
            {
                "code": "GST-5-FOOD-ESSENTIALS",
                "name": "GST 5% - Food Essentials",
                "tax_type": "GST",
                "rate": 5.0,
                "hsn_code": "1905",
                "applicable_business_types": ["RETAIL", "KIRANA"],
                "effective_from": "2017-07-01",
                "is_compound": true,
                "components": [
                    {"component_type": "CGST", "rate": 2.5, "account_code": "CGST_PAYABLE"},
                    {"component_type": "SGST", "rate": 2.5, "account_code": "SGST_PAYABLE"}
                ]
            }
            """,
            // 12% GST items
            """
            {
                "code": "GST-12-PROCESSED-GOODS", 
                "name": "GST 12% - Processed Goods",
                "tax_type": "GST",
                "rate": 12.0,
                "hsn_code": "2106",
                "applicable_business_types": ["RETAIL"],
                "effective_from": "2017-07-01",
                "is_compound": true,
                "components": [
                    {"component_type": "CGST", "rate": 6.0, "account_code": "CGST_PAYABLE"},
                    {"component_type": "SGST", "rate": 6.0, "account_code": "SGST_PAYABLE"}
                ]
            }
            """,
            // 18% GST items
            """
            {
                "code": "GST-18-GENERAL-GOODS",
                "name": "GST 18% - General Goods",
                "tax_type": "GST",
                "rate": 18.0,
                "hsn_code": "8517",
                "applicable_business_types": ["RETAIL"],
                "effective_from": "2017-07-01",
                "is_compound": true,
                "components": [
                    {"component_type": "CGST", "rate": 9.0, "account_code": "CGST_PAYABLE"},
                    {"component_type": "SGST", "rate": 9.0, "account_code": "SGST_PAYABLE"}
                ]
            }
            """,
            // 28% GST items with cess
            """
            {
                "code": "GST-28-LUXURY-CESS",
                "name": "GST 28% + Cess - Luxury Items",
                "tax_type": "GST",
                "rate": 28.0,
                "hsn_code": "8703",
                "applicable_business_types": ["RETAIL"],
                "effective_from": "2017-07-01",
                "is_compound": true,
                "cess_applicable": true,
                "cess_rate": 15.0,
                "components": [
                    {"component_type": "CGST", "rate": 14.0, "account_code": "CGST_PAYABLE"},
                    {"component_type": "SGST", "rate": 14.0, "account_code": "SGST_PAYABLE"},
                    {"component_type": "CESS", "rate": 15.0, "account_code": "CESS_PAYABLE"}
                ]
            }
            """
        )

        standardGstCodes.forEach { taxCodeJson ->
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", gstRegisteredWorkspaceId)
                .body(taxCodeJson.trimIndent())
                .`when`()
                .post("/tax-code/v1")
                .then()
                .statusCode(201)
        }

        // Setup composition scheme tax code for Kirana
        val compositionTaxCode = """
            {
                "code": "GST-COMPOSITION-1PERCENT",
                "name": "GST Composition 1% - Kirana Items",
                "tax_type": "GST_COMPOSITION",
                "rate": 1.0,
                "hsn_code": "COMP",
                "applicable_business_types": ["KIRANA"],
                "effective_from": "2017-07-01",
                "composition_scheme": true,
                "turnover_limit": 15000000.00
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .body(compositionTaxCode)
            .`when`()
            .post("/tax-code/v1")
            .then()
            .statusCode(201)

        // Setup precious metals tax codes for jewelry
        val preciousMetalsTaxCodes = listOf(
            """
            {
                "code": "GST-3-GOLD-JEWELRY",
                "name": "GST 3% - Gold Jewelry",
                "tax_type": "GST",
                "rate": 3.0,
                "hsn_code": "7113",
                "applicable_business_types": ["JEWELRY"],
                "effective_from": "2017-07-01",
                "is_compound": true,
                "special_provisions": {
                    "reverse_charge": false,
                    "tax_on_making_charges": true,
                    "wastage_taxable": false
                },
                "components": [
                    {"component_type": "CGST", "rate": 1.5, "account_code": "CGST_PAYABLE"},
                    {"component_type": "SGST", "rate": 1.5, "account_code": "SGST_PAYABLE"}
                ]
            }
            """,
            """
            {
                "code": "GST-12-SILVER-ARTICLES",
                "name": "GST 12% - Silver Articles",
                "tax_type": "GST",
                "rate": 12.0,
                "hsn_code": "7114",
                "applicable_business_types": ["JEWELRY"],
                "effective_from": "2017-07-01",
                "is_compound": true,
                "components": [
                    {"component_type": "CGST", "rate": 6.0, "account_code": "CGST_PAYABLE"},
                    {"component_type": "SGST", "rate": 6.0, "account_code": "SGST_PAYABLE"}
                ]
            }
            """
        )

        preciousMetalsTaxCodes.forEach { taxCodeJson ->
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", jewelryWorkspaceId)
                .body(taxCodeJson.trimIndent())
                .`when`()
                .post("/tax-code/v1")
                .then()
                .statusCode(201)
        }
    }

    @Test
    @Order(2)
    @DisplayName("Step 1: Test GST tax calculations for different rates and scenarios")
    fun `should perform accurate GST calculations for various business scenarios`() {
        // Test standard 18% GST calculation
        val gst18CalculationRequest = """
            {
                "tax_code": "GST-18-GENERAL-GOODS",
                "base_amount": 10000.00,
                "discount_amount": 500.00,
                "additional_charges": 200.00,
                "state_of_supply": "Delhi",
                "place_of_supply": "Delhi"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", gstRegisteredWorkspaceId)
            .body(gst18CalculationRequest)
            .`when`()
            .post("/tax-code/v1/calculate")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.taxable_amount", equalTo(9700.0f)) // 10000 - 500 + 200
            .body("data.cgst_amount", equalTo(873.0f)) // 9700 * 9%
            .body("data.sgst_amount", equalTo(873.0f)) // 9700 * 9%
            .body("data.igst_amount", equalTo(0.0f)) // Same state
            .body("data.total_tax", equalTo(1746.0f)) // CGST + SGST
            .body("data.total_amount", equalTo(11446.0f)) // 9700 + 1746

        // Test 28% GST with cess calculation
        val gst28CessCalculationRequest = """
            {
                "tax_code": "GST-28-LUXURY-CESS",
                "base_amount": 500000.00,
                "state_of_supply": "Delhi",
                "place_of_supply": "Delhi"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", gstRegisteredWorkspaceId)
            .body(gst28CessCalculationRequest)
            .`when`()
            .post("/tax-code/v1/calculate")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.taxable_amount", equalTo(500000.0f))
            .body("data.cgst_amount", equalTo(70000.0f)) // 500000 * 14%
            .body("data.sgst_amount", equalTo(70000.0f)) // 500000 * 14%
            .body("data.cess_amount", equalTo(75000.0f)) // 500000 * 15%
            .body("data.total_tax", equalTo(215000.0f)) // CGST + SGST + CESS
            .body("data.total_amount", equalTo(715000.0f))

        // Test inter-state GST (IGST) calculation
        val igstCalculationRequest = """
            {
                "tax_code": "GST-12-PROCESSED-GOODS",
                "base_amount": 25000.00,
                "state_of_supply": "Delhi",
                "place_of_supply": "Maharashtra"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", gstRegisteredWorkspaceId)
            .body(igstCalculationRequest)
            .`when`()
            .post("/tax-code/v1/calculate")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.taxable_amount", equalTo(25000.0f))
            .body("data.cgst_amount", equalTo(0.0f)) // Inter-state, no CGST
            .body("data.sgst_amount", equalTo(0.0f)) // Inter-state, no SGST
            .body("data.igst_amount", equalTo(3000.0f)) // 25000 * 12%
            .body("data.total_tax", equalTo(3000.0f))
    }

    @Test
    @Order(3)
    @DisplayName("Step 2: Test composition scheme calculations for small businesses")
    fun `should handle composition scheme tax calculations for kirana business`() {
        val compositionCalculationRequest = """
            {
                "tax_code": "GST-COMPOSITION-1PERCENT",
                "base_amount": 50000.00,
                "composition_scheme": true,
                "quarterly_turnover": 500000.00
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .body(compositionCalculationRequest)
            .`when`()
            .post("/tax-code/v1/calculate")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.scheme_type", equalTo("COMPOSITION"))
            .body("data.taxable_amount", equalTo(50000.0f))
            .body("data.composition_tax", equalTo(500.0f)) // 50000 * 1%
            .body("data.input_tax_credit_allowed", equalTo(false))
            .body("data.quarterly_limit_check", equalTo(true))
            .body("data.total_amount", equalTo(50500.0f))

        // Test composition scheme turnover limit validation
        val exceedingLimitRequest = """
            {
                "tax_code": "GST-COMPOSITION-1PERCENT",
                "base_amount": 100000.00,
                "composition_scheme": true,
                "quarterly_turnover": 4000000.00
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .body(exceedingLimitRequest)
            .`when`()
            .post("/tax-code/v1/calculate")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("COMPOSITION_LIMIT_EXCEEDED"))
            .body("error.message", containsString("quarterly turnover limit"))
    }

    @Test
    @Order(4)
    @DisplayName("Step 3: Test precious metals GST calculations with special provisions")
    fun `should handle precious metals GST calculations with making charges`() {
        // Test gold jewelry with making charges
        val goldJewelryCalculationRequest = """
            {
                "tax_code": "GST-3-GOLD-JEWELRY",
                "metal_value": 180000.00,
                "making_charges": 15000.00,
                "stone_value": 25000.00,
                "other_charges": 2000.00,
                "wastage_percentage": 8.0,
                "hallmark_charges": 500.00
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .body(goldJewelryCalculationRequest)
            .`when`()
            .post("/tax-code/v1/calculate")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.metal_value", equalTo(180000.0f))
            .body("data.making_charges_taxable", equalTo(15000.0f))
            .body("data.stone_value_taxable", equalTo(25000.0f))
            .body("data.wastage_non_taxable", equalTo(14400.0f)) // 8% of 180000
            .body("data.taxable_amount", equalTo(222500.0f)) // 180000+15000+25000+2000+500
            .body("data.cgst_amount", equalTo(3337.5f)) // 222500 * 1.5%
            .body("data.sgst_amount", equalTo(3337.5f)) // 222500 * 1.5%
            .body("data.total_tax", equalTo(6675.0f))
            .body("data.hallmark_compliance", equalTo(true))

        // Test silver articles calculation
        val silverCalculationRequest = """
            {
                "tax_code": "GST-12-SILVER-ARTICLES",
                "base_amount": 15000.00,
                "making_charges": 3000.00
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .body(silverCalculationRequest)
            .`when`()
            .post("/tax-code/v1/calculate")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.taxable_amount", equalTo(18000.0f))
            .body("data.cgst_amount", equalTo(1080.0f)) // 18000 * 6%
            .body("data.sgst_amount", equalTo(1080.0f)) // 18000 * 6%
            .body("data.total_tax", equalTo(2160.0f))
    }

    @Test
    @Order(5)
    @DisplayName("Step 4: Generate comprehensive GST reports for return filing")
    fun `should generate GST reports for GSTR filing`() {
        // Generate GSTR-1 (Sales) report
        val gstr1Request = """
            {
                "return_period": "$currentYear-$currentMonth",
                "return_type": "GSTR1",
                "include_b2b_invoices": true,
                "include_b2c_invoices": true,
                "include_exports": true,
                "include_nil_rated": true
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", gstRegisteredWorkspaceId)
            .body(gstr1Request)
            .`when`()
            .post("/gst/v1/generate-return")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.return_type", equalTo("GSTR1"))
            .body("data.return_period", equalTo("$currentYear-$currentMonth"))
            .body("data.gstin", equalTo("09AAACB1234C1Z5"))
            .body("data.b2b_invoices", notNullValue())
            .body("data.b2c_invoices", notNullValue())
            .body("data.summary.total_taxable_value", greaterThanOrEqualTo(0.0f))
            .body("data.summary.total_tax_amount", greaterThanOrEqualTo(0.0f))
            .body("data.json_ready_for_upload", equalTo(true))

        // Generate GSTR-3B (Monthly Summary) report
        val gstr3bRequest = """
            {
                "return_period": "$currentYear-$currentMonth",
                "return_type": "GSTR3B",
                "include_reverse_charge": true,
                "include_input_tax_credit": true
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", gstRegisteredWorkspaceId)
            .body(gstr3bRequest)
            .`when`()
            .post("/gst/v1/generate-return")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.return_type", equalTo("GSTR3B"))
            .body("data.outward_supplies.taxable_value", greaterThanOrEqualTo(0.0f))
            .body("data.outward_supplies.integrated_tax", greaterThanOrEqualTo(0.0f))
            .body("data.outward_supplies.central_tax", greaterThanOrEqualTo(0.0f))
            .body("data.outward_supplies.state_tax", greaterThanOrEqualTo(0.0f))
            .body("data.input_tax_credit.total_itc_claimed", greaterThanOrEqualTo(0.0f))
            .body("data.payment_of_tax.total_tax_payable", greaterThanOrEqualTo(0.0f))

        // Generate composition scheme return (GSTR-4)
        val gstr4Request = """
            {
                "return_period": "$currentYear-Q2",
                "return_type": "GSTR4",
                "composition_scheme": true
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .body(gstr4Request)
            .`when`()
            .post("/gst/v1/generate-return")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.return_type", equalTo("GSTR4"))
            .body("data.return_period", equalTo("$currentYear-Q2"))
            .body("data.composition_scheme", equalTo(true))
            .body("data.quarterly_summary.total_turnover", greaterThanOrEqualTo(0.0f))
            .body("data.quarterly_summary.total_tax", greaterThanOrEqualTo(0.0f))
    }

    @Test
    @Order(6)
    @DisplayName("Step 5: Test HSN/SAC code compliance and validation")
    fun `should validate HSN codes and ensure compliance`() {
        // Validate HSN code format and details
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", gstRegisteredWorkspaceId)
            .queryParam("hsn_code", "8517")
            .`when`()
            .get("/gst/v1/hsn-details")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.hsn_code", equalTo("8517"))
            .body("data.description", notNullValue())
            .body("data.gst_rate", notNullValue())
            .body("data.chapter", notNullValue())
            .body("data.valid", equalTo(true))

        // Get HSN summary for GST returns
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", gstRegisteredWorkspaceId)
            .queryParam("return_period", "$currentYear-$currentMonth")
            .`when`()
            .get("/gst/v1/hsn-summary")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.hsn_wise_summary", notNullValue())
            .body("data.total_hsn_codes", greaterThan(0))
            .body("data.compliance_status", equalTo("COMPLIANT"))

        // Validate precious metals HSN codes for jewelry business
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("hsn_code", "7113")
            .`when`()
            .get("/gst/v1/hsn-details")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.hsn_code", equalTo("7113"))
            .body("data.category", equalTo("Precious_Metals"))
            .body("data.special_provisions.hallmark_required", equalTo(true))
            .body("data.gst_rate", equalTo(3.0f))
    }

    @Test
    @Order(7)
    @DisplayName("Step 6: Test input tax credit (ITC) calculations and matching")
    fun `should calculate and validate input tax credit correctly`() {
        // Record purchase with ITC
        val purchaseWithItcRequest = """
            {
                "vendor_gstin": "27ABCDE1234F1Z5",
                "invoice_number": "PUR-2025-001",
                "invoice_date": "2025-09-07",
                "line_items": [
                    {
                        "description": "Raw materials for manufacturing",
                        "hsn_code": "3901",
                        "quantity": 100,
                        "rate": 500.00,
                        "taxable_value": 50000.00,
                        "cgst_amount": 4500.00,
                        "sgst_amount": 4500.00,
                        "igst_amount": 0.00
                    }
                ],
                "reverse_charge": false,
                "itc_eligible": true
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", gstRegisteredWorkspaceId)
            .body(purchaseWithItcRequest)
            .`when`()
            .post("/gst/v1/record-purchase")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.total_itc_amount", equalTo(9000.0f))
            .body("data.itc_cgst", equalTo(4500.0f))
            .body("data.itc_sgst", equalTo(4500.0f))
            .body("data.itc_eligible", equalTo(true))

        // Calculate net GST liability after ITC
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", gstRegisteredWorkspaceId)
            .queryParam("calculation_period", "$currentYear-$currentMonth")
            .`when`()
            .get("/gst/v1/net-liability")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.total_output_tax", greaterThanOrEqualTo(0.0f))
            .body("data.total_input_tax_credit", greaterThanOrEqualTo(0.0f))
            .body("data.net_gst_payable", greaterThanOrEqualTo(0.0f))
            .body("data.cgst_payable", greaterThanOrEqualTo(0.0f))
            .body("data.sgst_payable", greaterThanOrEqualTo(0.0f))
            .body("data.igst_payable", greaterThanOrEqualTo(0.0f))

        // Test ITC reversal for composition scheme
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .queryParam("composition_scheme", true)
            .`when`()
            .get("/gst/v1/itc-status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.itc_allowed", equalTo(false))
            .body("data.scheme_type", equalTo("COMPOSITION"))
            .body("data.message", containsString("Input tax credit not available"))
    }

    @Test
    @Order(8)
    @DisplayName("Step 7: Test GST rate change handling and transition management")
    fun `should handle GST rate changes and transition periods correctly`() {
        // Create tax code with future effective date (rate change)
        val futureRateChangeRequest = """
            {
                "code": "GST-18-TO-12-TRANSITION",
                "name": "GST Rate Change - 18% to 12%",
                "tax_type": "GST",
                "current_rate": 18.0,
                "new_rate": 12.0,
                "hsn_code": "2106",
                "effective_from": "2025-10-01",
                "transition_period": true,
                "applicable_business_types": ["RETAIL"]
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", gstRegisteredWorkspaceId)
            .body(futureRateChangeRequest)
            .`when`()
            .post("/tax-code/v1/rate-change")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.transition_scheduled", equalTo(true))
            .body("data.effective_from", equalTo("2025-10-01"))
            .body("data.current_rate", equalTo(18.0f))
            .body("data.new_rate", equalTo(12.0f))

        // Test rate applicability for different dates
        val rateCheckRequest = """
            {
                "tax_code": "GST-18-TO-12-TRANSITION",
                "transaction_date": "2025-09-15",
                "base_amount": 10000.00
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", gstRegisteredWorkspaceId)
            .body(rateCheckRequest)
            .`when`()
            .post("/tax-code/v1/calculate")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.applicable_rate", equalTo(18.0f)) // Before transition date
            .body("data.total_tax", equalTo(1800.0f))

        // Get rate change notifications
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", gstRegisteredWorkspaceId)
            .`when`()
            .get("/gst/v1/rate-change-notifications")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.upcoming_changes", hasSize(greaterThan(0)))
            .body("data.upcoming_changes[0].effective_date", equalTo("2025-10-01"))
            .body("data.upcoming_changes[0].impact_assessment", notNullValue())
    }

    @Test
    @Order(9)
    @DisplayName("Step 8: Test compliance validation and audit trail")
    fun `should maintain complete compliance audit trail and validation`() {
        // Validate overall GST compliance status
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", gstRegisteredWorkspaceId)
            .queryParam("compliance_period", "$currentYear-$currentMonth")
            .`when`()
            .get("/gst/v1/compliance-status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.overall_compliance", equalTo("COMPLIANT"))
            .body("data.gstr1_status", notNullValue())
            .body("data.gstr3b_status", notNullValue())
            .body("data.payment_status", notNullValue())
            .body("data.return_filing_dates.gstr1", notNullValue())
            .body("data.return_filing_dates.gstr3b", notNullValue())
            .body("data.late_fees_applicable", equalTo(false))

        // Get detailed audit trail for tax calculations
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("audit_period", "$currentYear-$currentMonth")
            .queryParam("include_calculations", true)
            .`when`()
            .get("/gst/v1/audit-trail")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.total_transactions", greaterThanOrEqualTo(0))
            .body("data.tax_calculations_verified", equalTo(true))
            .body("data.hsn_codes_validated", equalTo(true))
            .body("data.rate_applications_correct", equalTo(true))
            .body("data.precious_metals_compliance", equalTo(true))
            .body("data.hallmark_requirements_met", equalTo(true))

        // Validate composition scheme compliance
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .queryParam("scheme_validation", true)
            .`when`()
            .get("/gst/v1/compliance-status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.composition_scheme_compliance", equalTo("COMPLIANT"))
            .body("data.turnover_within_limits", equalTo(true))
            .body("data.no_itc_claimed", equalTo(true))
            .body("data.quarterly_returns_filed", equalTo(true))
    }

    @Test
    @Order(10)
    @DisplayName("Step 9: Test GST reconciliation and data integrity")
    fun `should provide GST reconciliation and ensure data integrity`() {
        // Reconcile sales data with GST returns
        val reconciliationRequest = """
            {
                "reconciliation_period": "$currentYear-$currentMonth",
                "reconcile_with": "GSTR1",
                "include_amendments": true
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", gstRegisteredWorkspaceId)
            .body(reconciliationRequest)
            .`when`()
            .post("/gst/v1/reconcile")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.reconciliation_status", equalTo("MATCHED"))
            .body("data.total_invoices_matched", greaterThanOrEqualTo(0))
            .body("data.discrepancies", hasSize(0))
            .body("data.tax_amount_matched", equalTo(true))
            .body("data.hsn_wise_reconciliation", equalTo("PASSED"))

        // Check for data integrity across all tax calculations
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", gstRegisteredWorkspaceId)
            .queryParam("integrity_check", true)
            .`when`()
            .get("/gst/v1/data-integrity")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.calculation_integrity", equalTo("VERIFIED"))
            .body("data.rate_application_correct", equalTo(true))
            .body("data.rounding_rules_followed", equalTo(true))
            .body("data.sequence_numbers_valid", equalTo(true))
            .body("data.duplicate_invoices_check", equalTo("PASSED"))

        // Generate compliance certificate
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("certificate_period", "$currentYear-$currentMonth")
            .`when`()
            .get("/gst/v1/compliance-certificate")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.certificate_issued", equalTo(true))
            .body("data.compliance_score", greaterThan(95.0f))
            .body("data.special_category_compliance.precious_metals", equalTo("COMPLIANT"))
            .body("data.certificate_valid_until", notNullValue())
    }

    @Test
    @Order(11)
    @DisplayName("Step 10: Validate complete GST compliance workflow across all business types")
    fun `should have complete GST compliance capabilities for all retail business types`() {
        // Validate regular GST compliance workflow
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", gstRegisteredWorkspaceId)
            .queryParam("workflow_validation", true)
            .`when`()
            .get("/gst/v1/workflow-status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.tax_calculation_ready", equalTo(true))
            .body("data.return_generation_ready", equalTo(true))
            .body("data.itc_processing_ready", equalTo(true))
            .body("data.compliance_monitoring_ready", equalTo(true))
            .body("data.rate_change_management_ready", equalTo(true))

        // Validate composition scheme workflow
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", kiranaWorkspaceId)
            .queryParam("composition_workflow", true)
            .`when`()
            .get("/gst/v1/workflow-status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.composition_calculation_ready", equalTo(true))
            .body("data.quarterly_return_ready", equalTo(true))
            .body("data.turnover_monitoring_ready", equalTo(true))
            .body("data.scheme_compliance_ready", equalTo(true))

        // Validate precious metals GST workflow
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("precious_metals_workflow", true)
            .`when`()
            .get("/gst/v1/workflow-status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.precious_metals_calculation_ready", equalTo(true))
            .body("data.hallmark_compliance_ready", equalTo(true))
            .body("data.making_charges_handling_ready", equalTo(true))
            .body("data.wastage_calculation_ready", equalTo(true))

        // Test performance across all GST operations
        listOf(gstRegisteredWorkspaceId, kiranaWorkspaceId, jewelryWorkspaceId).forEach { workspaceId ->
            RestAssured
                .given()
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", workspaceId)
                .queryParam("performance_test", true)
                .`when`()
                .get("/gst/v1/performance-status")
                .then()
                .statusCode(200)
                .time(lessThan(2000L)) // Should respond within 2 seconds
                .body("success", equalTo(true))
        }

        // Final comprehensive compliance validation
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", gstRegisteredWorkspaceId)
            .queryParam("comprehensive_check", true)
            .`when`()
            .get("/gst/v1/final-validation")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.all_business_types_supported", equalTo(true))
            .body("data.all_gst_rates_handled", equalTo(true))
            .body("data.all_return_types_supported", equalTo(true))
            .body("data.audit_trail_complete", equalTo(true))
            .body("data.compliance_score", greaterThan(98.0f))
    }
}