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

/**
 * Integration tests for complete invoice generation workflow.
 * 
 * Tests the end-to-end process of generating invoices from orders,
 * payment processing, PDF generation, and GST compliance across different business types.
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class InvoiceGenerationWorkflowIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    private var retailWorkspaceId: String = ""
    private var jewelryWorkspaceId: String = ""
    private var hardwareWorkspaceId: String = ""
    private var authToken: String = "valid_jwt_token"
    private var createdOrderIds: MutableList<String> = mutableListOf()
    private var createdInvoiceIds: MutableList<String> = mutableListOf()

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @Order(1)
    @DisplayName("Setup: Create test workspaces and complete orders for invoice generation")
    fun `should create test environments with completed orders`() {
        // Create retail workspace
        val retailWorkspaceRequest = """
            {
                "name": "City Center Retail",
                "description": "Modern retail store with POS system",
                "business_type": "RETAIL",
                "owner_details": {
                    "name": "Arjun Patel",
                    "phone": "+919876543260",
                    "email": "arjun@citycenter.com"
                },
                "business_details": {
                    "gstin": "24ABCDE1234F1Z5",
                    "pan": "ABCDE1234F"
                }
            }
        """.trimIndent()

        val retailResponse = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .body(retailWorkspaceRequest)
            .`when`()
            .post("/workspace/v1")
            .then()
            .statusCode(201)
            .extract()

        retailWorkspaceId = retailResponse.path("data.id")

        // Create jewelry workspace
        val jewelryWorkspaceRequest = """
            {
                "name": "Heritage Jewelers",
                "description": "Traditional and contemporary jewelry designs",
                "business_type": "JEWELRY",
                "owner_details": {
                    "name": "Meera Shah",
                    "phone": "+919876543261",
                    "email": "meera@heritagejewelers.com"
                },
                "business_details": {
                    "gstin": "27FGHIJ5678K1L2",
                    "pan": "FGHIJ5678K"
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

        // Create hardware workspace
        val hardwareWorkspaceRequest = """
            {
                "name": "Pro Hardware Solutions",
                "description": "Industrial hardware and construction supplies",
                "business_type": "HARDWARE",
                "owner_details": {
                    "name": "Vikram Singh",
                    "phone": "+919876543262",
                    "email": "vikram@prohardware.com"
                },
                "business_details": {
                    "gstin": "36KLMNO9012P3Q4",
                    "pan": "KLMNO9012P"
                }
            }
        """.trimIndent()

        val hardwareResponse = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .body(hardwareWorkspaceRequest)
            .`when`()
            .post("/workspace/v1")
            .then()
            .statusCode(201)
            .extract()

        hardwareWorkspaceId = hardwareResponse.path("data.id")

        // Setup products, customers, and completed orders for each workspace
        setupRetailEnvironment()
        setupJewelryEnvironment()
        setupHardwareEnvironment()
    }

    private fun setupRetailEnvironment() {
        // Create retail products
        val retailProducts = listOf(
            """{"name": "Samsung LED TV 32\"", "sku": "TV-SAMSUNG-32LED", "unit_id": "unit-pieces", "tax_code_id": "tax-gst-18", "base_price": 28000.00, "cost_price": 24000.00}""",
            """{"name": "Apple iPhone 14", "sku": "PHONE-IPHONE-14", "unit_id": "unit-pieces", "tax_code_id": "tax-gst-18", "base_price": 79900.00, "cost_price": 72000.00}""",
            """{"name": "Nike Running Shoes", "sku": "SHOES-NIKE-RUN", "unit_id": "unit-pieces", "tax_code_id": "tax-gst-12", "base_price": 4500.00, "cost_price": 3200.00}"""
        )

        retailProducts.forEach { productJson ->
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", retailWorkspaceId)
                .body(productJson)
                .`when`()
                .post("/product/v1")
                .then()
                .statusCode(201)
        }

        // Create retail customer
        val retailCustomerRequest = """
            {
                "name": "Rajesh Kumar",
                "phone": "+919876543270",
                "email": "rajesh.kumar@gmail.com",
                "customer_type": "RETAIL",
                "address": {
                    "street": "123 MG Road",
                    "city": "Mumbai",
                    "state": "Maharashtra",
                    "postal_code": "400001",
                    "country": "India"
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", retailWorkspaceId)
            .body(retailCustomerRequest)
            .`when`()
            .post("/customer/v1")
            .then()
            .statusCode(201)

        // Create and complete retail order
        createCompletedOrder(retailWorkspaceId, "RETAIL", "+919876543270", 
            listOf(
                """{"product_sku": "TV-SAMSUNG-32LED", "quantity": 1, "unit_price": 28000.00}""",
                """{"product_sku": "SHOES-NIKE-RUN", "quantity": 2, "unit_price": 4500.00}"""
            )
        )
    }

    private fun setupJewelryEnvironment() {
        // Create jewelry products
        val jewelryProducts = listOf(
            """{"name": "22K Gold Necklace Set", "sku": "GOLD-NECKLACE-SET-22K", "unit_id": "unit-pieces", "tax_code_id": "tax-gst-3", "base_price": 185000.00, "cost_price": 175000.00, "attributes": {"weight_grams": "35.5", "purity": "22K"}}""",
            """{"name": "Diamond Ring 1ct", "sku": "DIAMOND-RING-1CT", "unit_id": "unit-pieces", "tax_code_id": "tax-gst-3", "base_price": 350000.00, "cost_price": 320000.00, "attributes": {"diamond_carats": "1.0", "diamond_clarity": "VVS1"}}""",
            """{"name": "Silver Pooja Set", "sku": "SILVER-POOJA-SET", "unit_id": "unit-pieces", "tax_code_id": "tax-gst-12", "base_price": 8500.00, "cost_price": 7200.00}"""
        )

        jewelryProducts.forEach { productJson ->
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", jewelryWorkspaceId)
                .body(productJson)
                .`when`()
                .post("/product/v1")
                .then()
                .statusCode(201)
        }

        // Create jewelry customer with GST
        val jewelryCustomerRequest = """
            {
                "name": "Precious Moments Pvt Ltd",
                "phone": "+919876543271",
                "email": "orders@preciousmoments.com",
                "customer_type": "WHOLESALE",
                "business_name": "Precious Moments Pvt Ltd",
                "gst_number": "27RSTUB1234C1Z5",
                "credit_limit": 1000000.00,
                "credit_days": 30,
                "address": {
                    "street": "Jewelry Complex, Opera House",
                    "city": "Mumbai",
                    "state": "Maharashtra",
                    "postal_code": "400004",
                    "country": "India"
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .body(jewelryCustomerRequest)
            .`when`()
            .post("/customer/v1")
            .then()
            .statusCode(201)

        // Create and complete jewelry order
        createCompletedOrder(jewelryWorkspaceId, "JEWELRY", "+919876543271",
            listOf(
                """{"product_sku": "GOLD-NECKLACE-SET-22K", "quantity": 2, "unit_price": 185000.00}""",
                """{"product_sku": "SILVER-POOJA-SET", "quantity": 1, "unit_price": 8500.00}"""
            )
        )
    }

    private fun setupHardwareEnvironment() {
        // Create hardware products
        val hardwareProducts = listOf(
            """{"name": "Construction Steel TMT 16mm", "sku": "STEEL-TMT-16MM", "unit_id": "unit-tons", "tax_code_id": "tax-gst-18", "base_price": 58000.00, "cost_price": 55000.00}""",
            """{"name": "Cement Bags ACC 50kg", "sku": "CEMENT-ACC-50KG", "unit_id": "unit-bags", "tax_code_id": "tax-gst-28", "base_price": 350.00, "cost_price": 320.00}""",
            """{"name": "Power Tools Kit", "sku": "TOOLS-POWER-KIT", "unit_id": "unit-pieces", "tax_code_id": "tax-gst-18", "base_price": 12000.00, "cost_price": 9800.00}"""
        )

        hardwareProducts.forEach { productJson ->
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", hardwareWorkspaceId)
                .body(productJson)
                .`when`()
                .post("/product/v1")
                .then()
                .statusCode(201)
        }

        // Create hardware customer (contractor)
        val hardwareCustomerRequest = """
            {
                "name": "Metro Construction Ltd",
                "phone": "+919876543272",
                "email": "procurement@metroconstruction.com",
                "customer_type": "CONTRACTOR",
                "business_name": "Metro Construction Ltd",
                "gst_number": "36VWXYZ9012D1E2",
                "credit_limit": 2000000.00,
                "credit_days": 45,
                "address": {
                    "street": "Industrial Estate Phase 2",
                    "city": "Pune",
                    "state": "Maharashtra",
                    "postal_code": "411019",
                    "country": "India"
                }
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .body(hardwareCustomerRequest)
            .`when`()
            .post("/customer/v1")
            .then()
            .statusCode(201)

        // Create and complete hardware order
        createCompletedOrder(hardwareWorkspaceId, "HARDWARE", "+919876543272",
            listOf(
                """{"product_sku": "STEEL-TMT-16MM", "quantity": 2, "unit_price": 58000.00}""",
                """{"product_sku": "CEMENT-ACC-50KG", "quantity": 50, "unit_price": 350.00}""",
                """{"product_sku": "TOOLS-POWER-KIT", "quantity": 1, "unit_price": 12000.00}"""
            )
        )
    }

    private fun createCompletedOrder(workspaceId: String, businessType: String, customerPhone: String, lineItems: List<String>) {
        val orderRequest = """
            {
                "customer_phone": "$customerPhone",
                "line_items": [${lineItems.joinToString(",")}],
                "notes": "Order for invoice generation test - $businessType business"
            }
        """.trimIndent()

        val orderResponse = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", workspaceId)
            .body(orderRequest)
            .`when`()
            .post("/order/v1")
            .then()
            .statusCode(201)
            .extract()

        val orderId = orderResponse.path<String>("data.id")
        createdOrderIds.add(orderId)

        // Complete the order through all status transitions
        val statusTransitions = listOf("CONFIRMED", "PROCESSING", "READY", "COMPLETED")
        statusTransitions.forEach { status ->
            val statusRequest = if (status == "COMPLETED") {
                """{"new_status": "$status", "notes": "Order completed for invoice generation", "payment_method": "CASH", "payment_amount": ${orderResponse.path<Float>("data.total_amount")}}"""
            } else {
                """{"new_status": "$status", "notes": "Moving to $status status"}"""
            }

            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", workspaceId)
                .body(statusRequest)
                .`when`()
                .put("/order/v1/$orderId/status")
                .then()
                .statusCode(200)
        }
    }

    @Test
    @Order(2)
    @DisplayName("Step 1: Generate retail invoice with standard GST")
    fun `should generate retail invoice with 18 percent GST calculation`() {
        val retailOrderId = createdOrderIds[0]

        val invoiceRequest = """
            {
                "order_id": "$retailOrderId",
                "invoice_type": "RETAIL",
                "payment_method": "CASH",
                "notes": "Cash payment for electronics purchase",
                "generate_pdf": true
            }
        """.trimIndent()

        val response = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", retailWorkspaceId)
            .body(invoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.invoice_type", equalTo("RETAIL"))
            .body("data.invoice_number", matchesRegex("INV-\\d{8}-\\d{4}"))
            .body("data.line_items", hasSize(2))
            .body("data.line_items[0].tax_rate", equalTo(18.0f)) // TV has 18% GST
            .body("data.line_items[1].tax_rate", equalTo(12.0f)) // Shoes have 12% GST
            .body("data.subtotal", equalTo(37000.0f)) // 28000 + (2*4500)
            .body("data.tax_amount", greaterThan(4000.0f)) // Mix of 18% and 12% GST
            .body("data.total_amount", greaterThan(41000.0f))
            .body("data.payment_method", equalTo("CASH"))
            .body("data.status", equalTo("PAID"))
            .body("data.pdf_generated", equalTo(true))
            .body("data.pdf_url", notNullValue())
            .extract()

        val invoiceId = response.path<String>("data.id")
        createdInvoiceIds.add(invoiceId)
        println("Generated retail invoice: $invoiceId")
    }

    @Test
    @Order(3)
    @DisplayName("Step 2: Generate GST invoice for jewelry business with B2B compliance")
    fun `should generate GST compliant invoice for jewelry business`() {
        val jewelryOrderId = createdOrderIds[1]

        val gstInvoiceRequest = """
            {
                "order_id": "$jewelryOrderId",
                "invoice_type": "GST",
                "payment_method": "BANK_TRANSFER",
                "payment_reference": "NEFT123456789",
                "due_date": "2025-10-07",
                "notes": "B2B jewelry sale with GST compliance",
                "generate_pdf": true,
                "include_hsn_codes": true
            }
        """.trimIndent()

        val response = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .body(gstInvoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.invoice_type", equalTo("GST"))
            .body("data.customer.gst_number", equalTo("27RSTUB1234C1Z5"))
            .body("data.gst_details.supplier_gstin", equalTo("27FGHIJ5678K1L2"))
            .body("data.gst_details.customer_gstin", equalTo("27RSTUB1234C1Z5"))
            .body("data.gst_details.place_of_supply", equalTo("Maharashtra"))
            .body("data.line_items[0].hsn_code", notNullValue())
            .body("data.line_items[0].tax_rate", equalTo(3.0f)) // Gold jewelry has 3% GST
            .body("data.line_items[2].tax_rate", equalTo(12.0f)) // Silver items have 12% GST
            .body("data.tax_breakup.cgst", greaterThan(0.0f))
            .body("data.tax_breakup.sgst", greaterThan(0.0f))
            .body("data.tax_breakup.igst", equalTo(0.0f)) // Same state transaction
            .body("data.subtotal", equalTo(378500.0f)) // (2*185000) + 8500
            .body("data.hallmark_details", notNullValue()) // Jewelry specific details
            .body("data.precious_metal_details.total_gold_weight", equalTo(71.0f)) // 2 * 35.5g
            .body("data.status", equalTo("PENDING"))
            .body("data.due_date", equalTo("2025-10-07"))
            .extract()

        val invoiceId = response.path<String>("data.id")
        createdInvoiceIds.add(invoiceId)
        println("Generated GST jewelry invoice: $invoiceId")
    }

    @Test
    @Order(4)
    @DisplayName("Step 3: Generate hardware invoice with inter-state GST (IGST)")
    fun `should generate inter-state GST invoice for hardware business`() {
        val hardwareOrderId = createdOrderIds[2]

        val igstInvoiceRequest = """
            {
                "order_id": "$hardwareOrderId",
                "invoice_type": "GST",
                "payment_method": "CREDIT",
                "credit_days": 45,
                "delivery_state": "Karnataka",
                "notes": "Inter-state B2B construction materials sale",
                "generate_pdf": true,
                "include_transport_details": true,
                "transport_mode": "ROAD",
                "vehicle_number": "KA-09-AB-1234"
            }
        """.trimIndent()

        val response = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .body(igstInvoiceRequest)
            .`when`()
            .post("/invoice/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.invoice_type", equalTo("GST"))
            .body("data.gst_details.supplier_gstin", equalTo("36KLMNO9012P3Q4"))
            .body("data.gst_details.customer_gstin", equalTo("36VWXYZ9012D1E2"))
            .body("data.gst_details.place_of_supply", equalTo("Karnataka"))
            .body("data.gst_details.is_inter_state", equalTo(true))
            .body("data.tax_breakup.cgst", equalTo(0.0f)) // Inter-state, so no CGST/SGST
            .body("data.tax_breakup.sgst", equalTo(0.0f))
            .body("data.tax_breakup.igst", greaterThan(15000.0f)) // IGST for inter-state
            .body("data.subtotal", equalTo(145500.0f)) // (2*58000) + (50*350) + 12000
            .body("data.transport_details.mode", equalTo("ROAD"))
            .body("data.transport_details.vehicle_number", equalTo("KA-09-AB-1234"))
            .body("data.e_way_bill_required", equalTo(true)) // Hardware orders typically need e-way bill
            .body("data.status", equalTo("PENDING"))
            .extract()

        val invoiceId = response.path<String>("data.id")
        createdInvoiceIds.add(invoiceId)
        println("Generated hardware IGST invoice: $invoiceId")
    }

    @Test
    @Order(5)
    @DisplayName("Step 4: Process invoice payments and update status")
    fun `should process invoice payments and update payment status`() {
        val retailInvoiceId = createdInvoiceIds[0] // Already paid (CASH)
        val jewelryInvoiceId = createdInvoiceIds[1] // Pending payment
        val hardwareInvoiceId = createdInvoiceIds[2] // Credit payment

        // Process payment for jewelry invoice
        val jewelryPaymentRequest = """
            {
                "payment_method": "BANK_TRANSFER",
                "payment_amount": 389620.0,
                "payment_reference": "NEFT987654321",
                "payment_date": "2025-09-08",
                "notes": "Full payment received via NEFT"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .body(jewelryPaymentRequest)
            .`when`()
            .post("/invoice/v1/$jewelryInvoiceId/payments")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.payment_method", equalTo("BANK_TRANSFER"))
            .body("data.payment_amount", equalTo(389620.0f))
            .body("data.invoice_status", equalTo("PAID"))
            .body("data.remaining_balance", equalTo(0.0f))

        // Process partial payment for hardware invoice
        val hardwarePartialPaymentRequest = """
            {
                "payment_method": "CHEQUE",
                "payment_amount": 80000.0,
                "payment_reference": "CHEQUE789456123",
                "payment_date": "2025-09-10",
                "notes": "Partial payment - advance for materials"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .body(hardwarePartialPaymentRequest)
            .`when`()
            .post("/invoice/v1/$hardwareInvoiceId/payments")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.payment_amount", equalTo(80000.0f))
            .body("data.invoice_status", equalTo("PARTIALLY_PAID"))
            .body("data.remaining_balance", greaterThan(80000.0f))

        // Verify invoice status updates
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .`when`()
            .get("/invoice/v1/$jewelryInvoiceId")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("PAID"))
            .body("data.payment_history", hasSize(1))
    }

    @Test
    @Order(6)
    @DisplayName("Step 5: Generate and download invoice PDFs")
    fun `should generate and provide downloadable PDF invoices`() {
        val retailInvoiceId = createdInvoiceIds[0]
        val jewelryInvoiceId = createdInvoiceIds[1]

        // Download retail invoice PDF
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", retailWorkspaceId)
            .`when`()
            .get("/invoice/v1/$retailInvoiceId/pdf")
            .then()
            .statusCode(200)
            .contentType("application/pdf")
            .header("Content-Disposition", containsString("invoice"))
            .body(notNullValue())

        // Download jewelry GST invoice PDF with letterhead
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("include_letterhead", true)
            .queryParam("include_terms", true)
            .`when`()
            .get("/invoice/v1/$jewelryInvoiceId/pdf")
            .then()
            .statusCode(200)
            .contentType("application/pdf")
            .header("Content-Disposition", containsString("invoice"))

        // Generate custom invoice PDF with QR code
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("include_qr_code", true)
            .queryParam("include_digital_signature", true)
            .queryParam("format", "A4")
            .`when`()
            .get("/invoice/v1/$jewelryInvoiceId/pdf")
            .then()
            .statusCode(200)
            .contentType("application/pdf")
    }

    @Test
    @Order(7)
    @DisplayName("Step 6: Test invoice search and filtering capabilities")
    fun `should support comprehensive invoice search and filtering`() {
        // Search invoices by customer name
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("search", "Precious Moments")
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content.findAll { it.customer.business_name.contains('Precious') }", 
                  hasSize(greaterThan(0)))

        // Filter by payment status
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", retailWorkspaceId)
            .queryParam("status", "PAID")
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("status", "PAID")))

        // Filter by invoice type
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("invoice_type", "GST")
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("invoice_type", "GST")))

        // Filter by amount range
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("min_amount", 100000)
            .queryParam("max_amount", 500000)
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(allOf(
                hasEntry("total_amount", greaterThanOrEqualTo(100000.0f)),
                hasEntry("total_amount", lessThanOrEqualTo(500000.0f))
            )))

        // Filter overdue invoices
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .queryParam("overdue_only", true)
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
    }

    @Test
    @Order(8)
    @DisplayName("Step 7: Generate invoice analytics and reports")
    fun `should provide comprehensive invoice analytics and reporting`() {
        // Get overall invoice statistics
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", retailWorkspaceId)
            .queryParam("from_date", "2025-09-01")
            .queryParam("to_date", "2025-09-30")
            .queryParam("include_summary", true)
            .`when`()
            .get("/invoice/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.summary.total_invoices", greaterThan(0))
            .body("data.summary.total_amount", greaterThan(40000.0f))
            .body("data.summary.paid_amount", greaterThan(0.0f))
            .body("data.summary.pending_amount", greaterThanOrEqualTo(0.0f))

        // Get GST analytics for jewelry business
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("include_tax_analytics", true)
            .queryParam("from_date", "2025-09-01")
            .queryParam("to_date", "2025-09-30")
            .`when`()
            .get("/invoice/v1/analytics")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.gst_summary.total_cgst", greaterThan(0.0f))
            .body("data.gst_summary.total_sgst", greaterThan(0.0f))
            .body("data.gst_summary.total_igst", greaterThanOrEqualTo(0.0f))
            .body("data.gst_summary.input_tax_credit", notNullValue())
            .body("data.precious_metal_analytics.total_gold_sales", greaterThan(0.0f))

        // Get payment method analytics
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .queryParam("payment_analytics", true)
            .`when`()
            .get("/invoice/v1/analytics")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.payment_methods.cash_percentage", greaterThanOrEqualTo(0.0f))
            .body("data.payment_methods.credit_percentage", greaterThan(0.0f))
            .body("data.outstanding_amounts", notNullValue())
    }

    @Test
    @Order(9)
    @DisplayName("Step 8: Test invoice export and bulk operations")
    fun `should support invoice export and bulk operations`() {
        // Export invoices in Excel format
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("export_format", "EXCEL")
            .queryParam("from_date", "2025-09-01")
            .queryParam("to_date", "2025-09-30")
            .queryParam("include_gst_details", true)
            .`when`()
            .get("/invoice/v1/list/export")
            .then()
            .statusCode(200)
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .header("Content-Disposition", containsString("invoices_export"))

        // Bulk send invoice reminders
        val pendingInvoiceIds = createdInvoiceIds.takeLast(1) // Get pending invoices

        val bulkReminderRequest = """
            {
                "invoice_ids": ${pendingInvoiceIds.map { "\"$it\"" }},
                "reminder_type": "EMAIL",
                "custom_message": "Gentle reminder: Your invoice payment is due soon."
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .body(bulkReminderRequest)
            .`when`()
            .post("/invoice/v1/bulk/send-reminders")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.reminders_sent", greaterThan(0))
            .body("data.failed_reminders", hasSize(0))

        // Bulk PDF generation
        val bulkPdfRequest = """
            {
                "invoice_ids": ${createdInvoiceIds.take(2).map { "\"$it\"" }},
                "format": "A4",
                "include_letterhead": true
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", retailWorkspaceId)
            .body(bulkPdfRequest)
            .`when`()
            .post("/invoice/v1/bulk/generate-pdfs")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.pdfs_generated", greaterThan(0))
            .body("data.download_links", hasSize(greaterThan(0)))
    }

    @Test
    @Order(10)
    @DisplayName("Step 9: Validate invoice compliance and audit trail")
    fun `should maintain complete audit trail and compliance records`() {
        val jewelryInvoiceId = createdInvoiceIds[1]

        // Get complete invoice audit trail
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .`when`()
            .get("/invoice/v1/$jewelryInvoiceId/audit-trail")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.creation_details.created_by", notNullValue())
            .body("data.creation_details.created_at", notNullValue())
            .body("data.modifications", hasSize(greaterThanOrEqualTo(1))) // Payment update
            .body("data.pdf_generations", hasSize(greaterThan(0)))
            .body("data.access_history", hasSize(greaterThan(0)))

        // Validate GST compliance
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .`when`()
            .get("/invoice/v1/$jewelryInvoiceId/gst-compliance")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.gst_compliant", equalTo(true))
            .body("data.hsn_codes_present", equalTo(true))
            .body("data.tax_calculations_correct", equalTo(true))
            .body("data.gst_return_ready", equalTo(true))

        // Validate digital signature integrity
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .`when`()
            .get("/invoice/v1/$jewelryInvoiceId/verify-signature")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.signature_valid", equalTo(true))
            .body("data.tamper_proof", equalTo(true))
            .body("data.verification_timestamp", notNullValue())
    }

    @Test
    @Order(11)
    @DisplayName("Step 10: Validate complete invoice workflow across all business types")
    fun `should have complete invoice generation capabilities for all retail types`() {
        // Validate retail invoice workflow completeness
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", retailWorkspaceId)
            .queryParam("workflow_validation", true)
            .`when`()
            .get("/invoice/v1/workflow-status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.invoice_generation_ready", equalTo(true))
            .body("data.pdf_generation_ready", equalTo(true))
            .body("data.payment_processing_ready", equalTo(true))
            .body("data.tax_calculation_ready", equalTo(true))

        // Validate jewelry invoice workflow with precious metals
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("workflow_validation", true)
            .`when`()
            .get("/invoice/v1/workflow-status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.gst_b2b_ready", equalTo(true))
            .body("data.hallmark_compliance_ready", equalTo(true))
            .body("data.precious_metal_tracking_ready", equalTo(true))

        // Validate hardware invoice workflow with transportation
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .queryParam("workflow_validation", true)
            .`when`()
            .get("/invoice/v1/workflow-status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.inter_state_gst_ready", equalTo(true))
            .body("data.e_way_bill_ready", equalTo(true))
            .body("data.bulk_order_processing_ready", equalTo(true))

        // Test performance across all business types
        listOf(retailWorkspaceId, jewelryWorkspaceId, hardwareWorkspaceId).forEach { workspaceId ->
            RestAssured
                .given()
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", workspaceId)
                .queryParam("performance_mode", true)
                .`when`()
                .get("/invoice/v1/list")
                .then()
                .statusCode(200)
                .time(lessThan(2000L)) // Should respond within 2 seconds
                .body("success", equalTo(true))
        }
    }
}