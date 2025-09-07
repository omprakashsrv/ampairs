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
 * Contract tests for Customer Management API - Create Customer endpoint.
 * 
 * Tests verify the POST /customer/v1 endpoint according to the retail API contract.
 * Covers customer creation with retail business-specific fields and attributes.
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CustomerCreateContractTest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @DisplayName("POST /customer/v1 - Create basic retail customer")
    fun `should create basic customer with required fields`() {
        val customerRequest = """
            {
                "name": "Rajesh Kumar",
                "phone": "+919876543210",
                "email": "rajesh.kumar@example.com",
                "customer_type": "RETAIL",
                "address": {
                    "street": "123 MG Road",
                    "city": "Bangalore",
                    "state": "Karnataka",
                    "postal_code": "560001",
                    "country": "India"
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(customerRequest)
            .`when`()
            .post("/customer/v1")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("data.id", notNullValue())
            .body("data.customer_number", notNullValue())
            .body("data.name", equalTo("Rajesh Kumar"))
            .body("data.phone", equalTo("+919876543210"))
            .body("data.customer_type", equalTo("RETAIL"))
            .body("data.status", equalTo("ACTIVE"))
            .body("data.created_at", notNullValue())
            .body("data.address.city", equalTo("Bangalore"))
            .body("data.address.state", equalTo("Karnataka"))
    }

    @Test
    @DisplayName("POST /customer/v1 - Create kirana wholesale customer with credit")
    fun `should create wholesale customer with credit limit for kirana business`() {
        val wholesaleCustomerRequest = """
            {
                "name": "Sree Balaji General Stores",
                "phone": "+919123456789",
                "customer_type": "WHOLESALE",
                "business_name": "Sree Balaji General Stores",
                "gst_number": "29ABCDE1234F1Z5",
                "credit_limit": 50000.00,
                "credit_days": 30,
                "address": {
                    "street": "456 Commercial Street",
                    "city": "Mysore",
                    "state": "Karnataka", 
                    "postal_code": "570001",
                    "country": "India"
                },
                "attributes": {
                    "preferred_delivery_time": "MORNING",
                    "bulk_order_discount": 5.0,
                    "payment_terms": "NET_30"
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
            .body(wholesaleCustomerRequest)
            .`when`()
            .post("/customer/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.name", equalTo("Sree Balaji General Stores"))
            .body("data.customer_type", equalTo("WHOLESALE"))
            .body("data.business_name", equalTo("Sree Balaji General Stores"))
            .body("data.gst_number", equalTo("29ABCDE1234F1Z5"))
            .body("data.credit_limit", equalTo(50000.0f))
            .body("data.credit_days", equalTo(30))
            .body("data.attributes.bulk_order_discount", equalTo(5.0f))
            .body("data.attributes.payment_terms", equalTo("NET_30"))
    }

    @Test
    @DisplayName("POST /customer/v1 - Create jewelry customer with preferences")
    fun `should create jewelry customer with precious metal preferences`() {
        val jewelryCustomerRequest = """
            {
                "name": "Priya Sharma",
                "phone": "+919987654321", 
                "email": "priya.sharma@example.com",
                "customer_type": "RETAIL",
                "address": {
                    "street": "789 Brigade Road",
                    "city": "Bangalore",
                    "state": "Karnataka",
                    "postal_code": "560025",
                    "country": "India"
                },
                "attributes": {
                    "preferred_metal": "GOLD",
                    "preferred_purity": "22K",
                    "design_preferences": ["TRADITIONAL", "TEMPLE_JEWELRY"],
                    "size_preferences": {
                        "ring_size": "16",
                        "bangle_size": "2.4"
                    },
                    "special_occasions": ["WEDDING", "FESTIVAL"],
                    "budget_range": "HIGH"
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
            .body(jewelryCustomerRequest)
            .`when`()
            .post("/customer/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.name", equalTo("Priya Sharma"))
            .body("data.attributes.preferred_metal", equalTo("GOLD"))
            .body("data.attributes.preferred_purity", equalTo("22K"))
            .body("data.attributes.design_preferences", hasItems("TRADITIONAL", "TEMPLE_JEWELRY"))
            .body("data.attributes.size_preferences.ring_size", equalTo("16"))
            .body("data.attributes.budget_range", equalTo("HIGH"))
    }

    @Test
    @DisplayName("POST /customer/v1 - Create hardware contractor customer")
    fun `should create contractor customer with project details for hardware business`() {
        val contractorCustomerRequest = """
            {
                "name": "Bangalore Construction Co.",
                "phone": "+918765432109",
                "email": "contact@bangaloreconst.com",
                "customer_type": "CONTRACTOR",
                "business_name": "Bangalore Construction Co. Pvt Ltd",
                "gst_number": "29FGHIJ5678K1L2",
                "credit_limit": 100000.00,
                "credit_days": 45,
                "address": {
                    "street": "101 Industrial Area",
                    "city": "Bangalore",
                    "state": "Karnataka",
                    "postal_code": "560068",
                    "country": "India"
                },
                "attributes": {
                    "project_types": ["RESIDENTIAL", "COMMERCIAL"],
                    "preferred_brands": ["TATA", "BIRLA", "ACC"],
                    "delivery_requirements": "SITE_DELIVERY",
                    "payment_terms": "NET_45",
                    "volume_discount_tier": "PREMIUM"
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_HARDWARE_WS_001")
            .body(contractorCustomerRequest)
            .`when`()
            .post("/customer/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.name", equalTo("Bangalore Construction Co."))
            .body("data.customer_type", equalTo("CONTRACTOR"))
            .body("data.business_name", equalTo("Bangalore Construction Co. Pvt Ltd"))
            .body("data.attributes.project_types", hasItems("RESIDENTIAL", "COMMERCIAL"))
            .body("data.attributes.preferred_brands", hasItems("TATA", "BIRLA", "ACC"))
            .body("data.attributes.delivery_requirements", equalTo("SITE_DELIVERY"))
    }

    @Test
    @DisplayName("POST /customer/v1 - Validation error for missing required fields")
    fun `should return validation error when required fields are missing`() {
        val invalidCustomerRequest = """
            {
                "email": "incomplete@example.com",
                "customer_type": "RETAIL"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidCustomerRequest)
            .`when`()
            .post("/customer/v1")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.validation_errors.name", containsString("required"))
            .body("error.validation_errors.phone", containsString("required"))
    }

    @Test
    @DisplayName("POST /customer/v1 - Duplicate phone number validation")
    fun `should return conflict error when phone number already exists in workspace`() {
        val duplicatePhoneRequest = """
            {
                "name": "Duplicate Customer",
                "phone": "+919876543210",
                "customer_type": "RETAIL",
                "address": {
                    "street": "Test Street",
                    "city": "Test City",
                    "state": "Test State",
                    "postal_code": "123456",
                    "country": "India"
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(duplicatePhoneRequest)
            .`when`()
            .post("/customer/v1")
            .then()
            .statusCode(409)
            .body("success", equalTo(false))
            .body("error.code", equalTo("DUPLICATE_ENTRY"))
            .body("error.message", containsString("phone number"))
            .body("error.details", containsString("+919876543210"))
    }

    @Test
    @DisplayName("POST /customer/v1 - Multi-tenant isolation validation")
    fun `should allow same phone number in different workspaces`() {
        val samePhoneRequest = """
            {
                "name": "Same Phone Different Workspace",
                "phone": "+919999999999",
                "customer_type": "RETAIL",
                "address": {
                    "street": "Same Phone Street",
                    "city": "Same Phone City", 
                    "state": "Same Phone State",
                    "postal_code": "999999",
                    "country": "India"
                }
            }
        """.trimIndent()

        // Create in first workspace
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_A")
            .body(samePhoneRequest)
            .`when`()
            .post("/customer/v1")
            .then()
            .statusCode(201)

        // Create same phone in second workspace - should succeed
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_B")
            .body(samePhoneRequest)
            .`when`()
            .post("/customer/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
    }

    @Test
    @DisplayName("POST /customer/v1 - Invalid phone number format validation")
    fun `should validate phone number format`() {
        val invalidPhoneRequest = """
            {
                "name": "Invalid Phone Customer",
                "phone": "invalid-phone",
                "customer_type": "RETAIL",
                "address": {
                    "street": "Test Street",
                    "city": "Test City",
                    "state": "Test State",
                    "postal_code": "123456",
                    "country": "India"
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidPhoneRequest)
            .`when`()
            .post("/customer/v1")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.validation_errors.phone", containsString("invalid format"))
    }

    @Test
    @DisplayName("POST /customer/v1 - Invalid GST number format validation")
    fun `should validate GST number format for business customers`() {
        val invalidGstRequest = """
            {
                "name": "Invalid GST Business",
                "phone": "+919123456789",
                "customer_type": "WHOLESALE",
                "business_name": "Invalid GST Business",
                "gst_number": "INVALID-GST-FORMAT",
                "address": {
                    "street": "Business Street",
                    "city": "Business City",
                    "state": "Business State",
                    "postal_code": "123456",
                    "country": "India"
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidGstRequest)
            .`when`()
            .post("/customer/v1")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.validation_errors.gst_number", containsString("invalid format"))
    }

    @Test
    @DisplayName("POST /customer/v1 - Customer number generation")
    fun `should generate unique customer number following workspace pattern`() {
        val customerRequest = """
            {
                "name": "Customer Number Test",
                "phone": "+919111111111",
                "customer_type": "RETAIL",
                "address": {
                    "street": "Number Test Street",
                    "city": "Number Test City",
                    "state": "Number Test State",
                    "postal_code": "111111",
                    "country": "India"
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(customerRequest)
            .`when`()
            .post("/customer/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.customer_number", matchesRegex("CUST-\\d{8}-\\d{3}")) // Format: CUST-YYYYMMDD-001
    }

    @Test
    @DisplayName("POST /customer/v1 - Email uniqueness validation within workspace")
    fun `should validate email uniqueness within workspace`() {
        val duplicateEmailRequest = """
            {
                "name": "Duplicate Email Customer",
                "phone": "+919222222222",
                "email": "existing@example.com",
                "customer_type": "RETAIL",
                "address": {
                    "street": "Duplicate Email Street",
                    "city": "Duplicate Email City",
                    "state": "Duplicate Email State",
                    "postal_code": "222222",
                    "country": "India"
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(duplicateEmailRequest)
            .`when`()
            .post("/customer/v1")
            .then()
            .statusCode(409)
            .body("success", equalTo(false))
            .body("error.code", equalTo("DUPLICATE_ENTRY"))
            .body("error.message", containsString("email"))
    }

    @Test
    @DisplayName("POST /customer/v1 - Customer with loyalty program enrollment")
    fun `should create customer with automatic loyalty program enrollment`() {
        val loyaltyCustomerRequest = """
            {
                "name": "Loyalty Program Customer",
                "phone": "+919333333333",
                "email": "loyalty@example.com",
                "customer_type": "RETAIL",
                "enroll_in_loyalty": true,
                "address": {
                    "street": "Loyalty Street",
                    "city": "Loyalty City",
                    "state": "Loyalty State",
                    "postal_code": "333333",
                    "country": "India"
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(loyaltyCustomerRequest)
            .`when`()
            .post("/customer/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.loyalty_program.enrolled", equalTo(true))
            .body("data.loyalty_program.member_id", notNullValue())
            .body("data.loyalty_program.tier", equalTo("BRONZE"))
            .body("data.loyalty_program.points_balance", equalTo(0))
            .body("data.loyalty_program.enrollment_date", notNullValue())
    }

    @Test
    @DisplayName("POST /customer/v1 - Customer creation audit logging")
    fun `should track user who created the customer`() {
        val customerRequest = """
            {
                "name": "Audit Test Customer",
                "phone": "+919444444444",
                "customer_type": "RETAIL",
                "address": {
                    "street": "Audit Street",
                    "city": "Audit City",
                    "state": "Audit State", 
                    "postal_code": "444444",
                    "country": "India"
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(customerRequest)
            .`when`()
            .post("/customer/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.created_by.id", notNullValue())
            .body("data.created_by.name", notNullValue())
            .body("data.created_at", notNullValue())
            .body("data.updated_at", notNullValue())
    }
}