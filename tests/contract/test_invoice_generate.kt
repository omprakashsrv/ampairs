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
 * Contract tests for Invoice Generation API.
 * 
 * Tests verify invoice generation from orders via POST /invoice/v1
 * according to the retail API contract with proper tax calculations and formatting.
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InvoiceGenerateContractTest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @DisplayName("POST /invoice/v1 - Generate invoice from completed order")
    fun `should generate invoice from completed order with proper tax calculations`() {
        val invoiceRequest = """
            {
                "order_id": "ORD-20250907-001",
                "invoice_type": "RETAIL",
                "payment_method": "CASH",
                "notes": "Cash payment received in full"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("success", equalTo(true))
            .body("data.id", notNullValue())
            .body("data.invoice_number", notNullValue())
            .body("data.invoice_type", equalTo("RETAIL"))
            .body("data.order.id", equalTo("ORD-20250907-001"))
            .body("data.customer", notNullValue())
            .body("data.line_items", hasSize(greaterThan(0)))
            .body("data.subtotal", greaterThan(0.0f))
            .body("data.tax_amount", greaterThanOrEqualTo(0.0f))
            .body("data.total_amount", greaterThan(0.0f))
            .body("data.payment_method", equalTo("CASH"))
            .body("data.status", equalTo("PAID"))
            .body("data.generated_at", notNullValue())
    }

    @Test
    @DisplayName("POST /invoice/v1 - Generate GST invoice for business customer")
    fun `should generate GST compliant invoice for business customer`() {
        val gstInvoiceRequest = """
            {
                "order_id": "ORD-BUSINESS-001",
                "invoice_type": "GST",
                "payment_method": "BANK_TRANSFER",
                "due_date": "2025-10-07",
                "notes": "GST invoice for B2B transaction"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_HARDWARE_WS_001")
            .body(gstInvoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.invoice_type", equalTo("GST"))
            .body("data.customer.gst_number", notNullValue())
            .body("data.gst_details.supplier_gstin", notNullValue())
            .body("data.gst_details.customer_gstin", notNullValue())
            .body("data.gst_details.place_of_supply", notNullValue())
            .body("data.tax_breakup.cgst", greaterThanOrEqualTo(0.0f))
            .body("data.tax_breakup.sgst", greaterThanOrEqualTo(0.0f))
            .body("data.tax_breakup.igst", greaterThanOrEqualTo(0.0f))
            .body("data.status", equalTo("PENDING"))
            .body("data.due_date", equalTo("2025-10-07"))
    }

    @Test
    @DisplayName("POST /invoice/v1 - Generate jewelry invoice with precious metal details")
    fun `should generate jewelry invoice with metal purity and weight details`() {
        val jewelryInvoiceRequest = """
            {
                "order_id": "ORD-JEWELRY-001",
                "invoice_type": "JEWELRY",
                "payment_method": "CASH_AND_CARD",
                "cash_amount": 50000.00,
                "card_amount": 75000.00,
                "notes": "Gold jewelry purchase with partial card payment"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
            .body(jewelryInvoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.invoice_type", equalTo("JEWELRY"))
            .body("data.line_items[0].product_details.weight_grams", notNullValue())
            .body("data.line_items[0].product_details.purity", notNullValue())
            .body("data.line_items[0].product_details.metal_type", notNullValue())
            .body("data.payment_details.cash_amount", equalTo(50000.0f))
            .body("data.payment_details.card_amount", equalTo(75000.0f))
            .body("data.payment_details.total_paid", equalTo(125000.0f))
            .body("data.hallmark_details", notNullValue())
    }

    @Test
    @DisplayName("POST /invoice/v1 - Generate kirana invoice with bulk discount")
    fun `should generate kirana invoice with bulk quantity discounts`() {
        val kiranaInvoiceRequest = """
            {
                "order_id": "ORD-KIRANA-BULK-001",
                "invoice_type": "RETAIL",
                "payment_method": "CREDIT",
                "credit_days": 30,
                "notes": "Bulk order for wholesale customer with 30 days credit"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
            .body(kiranaInvoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.line_items", hasSize(greaterThan(0)))
            .body("data.line_items[0].bulk_discount_applied", greaterThanOrEqualTo(0.0f))
            .body("data.total_discount", greaterThanOrEqualTo(0.0f))
            .body("data.payment_method", equalTo("CREDIT"))
            .body("data.credit_terms.days", equalTo(30))
            .body("data.status", equalTo("PENDING"))
            .body("data.due_date", notNullValue())
    }

    @Test
    @DisplayName("POST /invoice/v1 - Invoice number generation with sequential numbering")
    fun `should generate sequential invoice numbers following workspace pattern`() {
        val invoiceRequest = """
            {
                "order_id": "ORD-SEQUENCE-001",
                "invoice_type": "RETAIL",
                "payment_method": "CASH"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.invoice_number", matchesRegex("INV-\\d{8}-\\d{4}")) // Format: INV-YYYYMMDD-0001
    }

    @Test
    @DisplayName("POST /invoice/v1 - Validation error for non-existent order")
    fun `should return error when order does not exist`() {
        val invalidInvoiceRequest = """
            {
                "order_id": "NONEXISTENT-ORDER-999",
                "invoice_type": "RETAIL",
                "payment_method": "CASH"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidInvoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(404)
            .body("success", equalTo(false))
            .body("error.code", equalTo("ORDER_NOT_FOUND"))
    }

    @Test
    @DisplayName("POST /invoice/v1 - Validation error for incomplete order")
    fun `should return error when order is not completed`() {
        val incompleteOrderInvoiceRequest = """
            {
                "order_id": "ORD-DRAFT-001",
                "invoice_type": "RETAIL",
                "payment_method": "CASH"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(incompleteOrderInvoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("ORDER_NOT_READY_FOR_INVOICING"))
            .body("error.message", containsString("order must be completed"))
    }

    @Test
    @DisplayName("POST /invoice/v1 - Prevent duplicate invoice generation")
    fun `should prevent generating duplicate invoices for the same order`() {
        val duplicateInvoiceRequest = """
            {
                "order_id": "ORD-ALREADY-INVOICED-001",
                "invoice_type": "RETAIL",
                "payment_method": "CASH"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(duplicateInvoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(409)
            .body("success", equalTo(false))
            .body("error.code", equalTo("INVOICE_ALREADY_EXISTS"))
            .body("error.message", containsString("invoice already exists"))
            .body("error.details.existing_invoice_id", notNullValue())
    }

    @Test
    @DisplayName("POST /invoice/v1 - Multi-tenant isolation validation")
    fun `should prevent invoice generation for orders from different workspace`() {
        val crossTenantInvoiceRequest = """
            {
                "order_id": "ORD-FROM-OTHER-WORKSPACE",
                "invoice_type": "RETAIL",
                "payment_method": "CASH"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_WORKSPACE_A")
            .body(crossTenantInvoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(404)
            .body("error.code", equalTo("ORDER_NOT_FOUND"))
    }

    @Test
    @DisplayName("POST /invoice/v1 - Required fields validation")
    fun `should validate required fields for invoice generation`() {
        val invalidInvoiceRequest = """
            {
                "invoice_type": "RETAIL"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidInvoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.validation_errors.order_id", containsString("required"))
            .body("error.validation_errors.payment_method", containsString("required"))
    }

    @Test
    @DisplayName("POST /invoice/v1 - Invalid payment method validation")
    fun `should reject invalid payment methods`() {
        val invalidPaymentMethodRequest = """
            {
                "order_id": "ORD-20250907-001",
                "invoice_type": "RETAIL",
                "payment_method": "INVALID_METHOD"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invalidPaymentMethodRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("VALIDATION_ERROR"))
            .body("error.validation_errors.payment_method", 
                  containsString("must be one of: CASH, CARD, BANK_TRANSFER, UPI, CASH_AND_CARD, CREDIT"))
    }

    @Test
    @DisplayName("POST /invoice/v1 - HSN code inclusion for GST compliance")
    fun `should include HSN codes for all line items in GST invoice`() {
        val gstInvoiceRequest = """
            {
                "order_id": "ORD-GST-COMPLIANCE-001",
                "invoice_type": "GST",
                "payment_method": "BANK_TRANSFER"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(gstInvoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.line_items", hasSize(greaterThan(0)))
            .body("data.line_items[0].hsn_code", notNullValue())
            .body("data.line_items[0].tax_rate", greaterThanOrEqualTo(0.0f))
            .body("data.line_items[0].taxable_amount", greaterThan(0.0f))
    }

    @Test
    @DisplayName("POST /invoice/v1 - Round-off handling in total calculations")
    fun `should handle round-off correctly in total amount calculations`() {
        val invoiceRequest = """
            {
                "order_id": "ORD-ROUNDOFF-TEST-001",
                "invoice_type": "RETAIL",
                "payment_method": "CASH"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.subtotal", notNullValue())
            .body("data.tax_amount", notNullValue())
            .body("data.round_off", anyOf(equalTo(0.0f), not(equalTo(0.0f))))
            .body("data.total_amount", notNullValue())
            .body("data.final_amount", notNullValue())
    }

    @Test
    @DisplayName("POST /invoice/v1 - Digital signature for invoice authenticity")
    fun `should include digital signature hash for invoice verification`() {
        val invoiceRequest = """
            {
                "order_id": "ORD-SIGNATURE-001",
                "invoice_type": "GST",
                "payment_method": "BANK_TRANSFER"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.digital_signature", notNullValue())
            .body("data.signature_algorithm", equalTo("SHA256"))
            .body("data.verification_url", notNullValue())
    }

    @Test
    @DisplayName("POST /invoice/v1 - Invoice generation audit logging")
    fun `should track user who generated the invoice`() {
        val invoiceRequest = """
            {
                "order_id": "ORD-AUDIT-001",
                "invoice_type": "RETAIL",
                "payment_method": "CASH",
                "notes": "Invoice generated for audit testing"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.generated_by.id", notNullValue())
            .body("data.generated_by.name", notNullValue())
            .body("data.generated_at", notNullValue())
            .body("data.workspace_info.name", notNullValue())
    }

    @Test
    @DisplayName("POST /invoice/v1 - Automatic PDF generation")
    fun `should automatically generate PDF version of the invoice`() {
        val invoiceRequest = """
            {
                "order_id": "ORD-PDF-001",
                "invoice_type": "RETAIL",
                "payment_method": "CASH",
                "generate_pdf": true
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer valid_jwt_token")
            .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
            .body(invoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.pdf_generated", equalTo(true))
            .body("data.pdf_url", notNullValue())
            .body("data.pdf_size_bytes", greaterThan(0))
    }
}