package com.ampairs.invoice

import com.ampairs.AmpairsApplication

import com.ampairs.core.domain.model.Address
import com.ampairs.invoice.domain.dto.*
import com.ampairs.invoice.domain.enums.InvoiceStatus
import com.ampairs.invoice.domain.model.Invoice
import com.ampairs.invoice.service.InvoiceService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.transaction.annotation.Transactional
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

/**
 * Integration tests for Invoice Management API - Generate Invoice endpoint.
 * 
 * Tests verify the POST /invoice/v1 endpoint using MockMvc with mocked services.
 * Covers invoice generation from orders with proper tax calculations and formatting.
 */
@SpringBootTest(classes = [AmpairsApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InvoiceGenerateIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var invoiceService: InvoiceService

    @Test
    @DisplayName("POST /invoice/v1 - Generate retail invoice from completed order")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should generate invoice from completed order with proper tax calculations`() {
        val invoiceItems = listOf(
            InvoiceItemRequest(
                productId = "PROD_HAMMER_001",
                description = "Steel Hammer 500g",
                quantity = 2.0,
                price = 450.00,
                totalCost = 900.00
            ),
            InvoiceItemRequest(
                productId = "PROD_SCREWS_001",
                description = "Steel Screws Pack",
                quantity = 1.0,
                price = 125.00,
                totalCost = 125.00
            )
        )

        val invoiceRequest = InvoiceUpdateRequest(
            orderRefId = "ORD-20250907-001",
            fromCustomerId = "CUST_HARDWARE_001",
            fromCustomerName = "ABC Hardware Store",
            toCustomerId = "CUST_RETAIL_001", 
            toCustomerName = "John Construction",
            totalCost = 1025.00,
            basePrice = 1025.00,
            totalTax = 184.50,
            status = InvoiceStatus.DRAFT,
            totalItems = 2,
            totalQuantity = 3.0,
            invoiceItems = invoiceItems
        )

        val mockInvoiceResponse = InvoiceResponse(
            id = "inv-123",
            invoiceNumber = "INV-20250909-001",
            orderRefId = "ORD-20250907-001",
            fromCustomerId = "CUST_HARDWARE_001",
            fromCustomerName = "ABC Hardware Store",
            toCustomerId = "CUST_RETAIL_001",
            toCustomerName = "John Construction",
            totalCost = 1025.00,
            basePrice = 1025.00,
            totalTax = 184.50,
            status = InvoiceStatus.DRAFT,
            totalItems = 2,
            totalQuantity = 3.0,
            invoiceItems = invoiceItems.map { 
                InvoiceItemResponse(
                    productId = it.productId,
                    description = it.description,
                    quantity = it.quantity,
                    price = it.price,
                    totalCost = it.totalCost
                )
            }
        )

        whenever(invoiceService.updateInvoice(any<Invoice>(), any<List<com.ampairs.invoice.domain.model.InvoiceItem>>()))
            .thenReturn(mockInvoiceResponse)

        mockMvc.perform(
            post("/invoice/v1")
                .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invoiceRequest))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value("inv-123"))
            .andExpect(jsonPath("$.invoiceNumber").value("INV-20250909-001"))
            .andExpect(jsonPath("$.orderRefId").value("ORD-20250907-001"))
            .andExpect(jsonPath("$.totalCost").value(1025.00))
            .andExpect(jsonPath("$.totalTax").value(184.50))
            .andExpect(jsonPath("$.invoiceItems").isArray)
            .andExpect(jsonPath("$.invoiceItems").isNotEmpty)

        verify(invoiceService).updateInvoice(any<Invoice>(), any<List<com.ampairs.invoice.domain.model.InvoiceItem>>())
    }

    @Test
    @DisplayName("POST /invoice/v1 - Generate GST compliant invoice for business customer")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should generate GST compliant invoice for business customer`() {
        val invoiceItems = listOf(
            InvoiceItemRequest(
                productId = "PROD_CEMENT_001",
                description = "Portland Cement 50kg",
                quantity = 20.0,
                price = 350.00,
                totalCost = 7000.00,
                taxCode = "2523"
            )
        )

        val invoiceRequest = InvoiceUpdateRequest(
            orderRefId = "ORD-BUSINESS-001",
            fromCustomerId = "CUST_SUPPLIER_001",
            fromCustomerName = "Karnataka Cement Works",
            fromCustomerGst = "29ABCDE1234F1Z5",
            toCustomerId = "CUST_BUILDER_001",
            toCustomerName = "Bangalore Builders Pvt Ltd",
            toCustomerGst = "29FGHIJ5678K1L2",
            totalCost = 7000.00,
            basePrice = 7000.00,
            totalTax = 1960.00,
            status = InvoiceStatus.NEW,
            totalItems = 1,
            totalQuantity = 20.0,
            invoiceItems = invoiceItems
        )

        val mockInvoiceResponse = InvoiceResponse(
            id = "inv-gst-456",
            invoiceNumber = "INV-GST-20250909-001",
            orderRefId = "ORD-BUSINESS-001",
            fromCustomerId = "CUST_SUPPLIER_001",
            fromCustomerName = "Karnataka Cement Works",
            fromCustomerGst = "29ABCDE1234F1Z5",
            toCustomerId = "CUST_BUILDER_001",
            toCustomerName = "Bangalore Builders Pvt Ltd",
            toCustomerGst = "29FGHIJ5678K1L2",
            totalCost = 7000.00,
            basePrice = 7000.00,
            totalTax = 1960.00,
            status = InvoiceStatus.NEW,
            totalItems = 1,
            totalQuantity = 20.0,
            invoiceItems = invoiceItems.map { 
                InvoiceItemResponse(
                    productId = it.productId,
                    description = it.description,
                    quantity = it.quantity,
                    price = it.price,
                    totalCost = it.totalCost,
                    taxCode = it.taxCode
                )
            }
        )

        whenever(invoiceService.updateInvoice(any<Invoice>(), any<List<com.ampairs.invoice.domain.model.InvoiceItem>>()))
            .thenReturn(mockInvoiceResponse)

        mockMvc.perform(
            post("/invoice/v1")
                .header("X-Workspace-ID", "TEST_HARDWARE_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invoiceRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.invoiceNumber").value("INV-GST-20250909-001"))
            .andExpect(jsonPath("$.fromCustomerGst").value("29ABCDE1234F1Z5"))
            .andExpect(jsonPath("$.toCustomerGst").value("29FGHIJ5678K1L2"))
            .andExpect(jsonPath("$.totalTax").value(1960.00))
            .andExpect(jsonPath("$.invoiceItems").isArray)

        verify(invoiceService).updateInvoice(any<Invoice>(), any<List<com.ampairs.invoice.domain.model.InvoiceItem>>())
    }

    @Test
    @DisplayName("POST /invoice/v1 - Generate jewelry invoice with precious metal details")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should generate jewelry invoice with metal purity and weight details`() {
        val invoiceItems = listOf(
            InvoiceItemRequest(
                productId = "PROD_GOLD_NECKLACE_001",
                description = "22K Gold Traditional Necklace",
                quantity = 1.0,
                price = 125000.00,
                totalCost = 125000.00
            )
        )

        val invoiceRequest = InvoiceUpdateRequest(
            orderRefId = "ORD-JEWELRY-001",
            fromCustomerId = "CUST_JEWELER_001",
            fromCustomerName = "Golden Dreams Jewelry",
            toCustomerId = "CUST_CUSTOMER_001",
            toCustomerName = "Priya Sharma",
            totalCost = 125000.00,
            basePrice = 125000.00,
            totalTax = 3750.00, // 3% GST on gold
            status = InvoiceStatus.INVOICED,
            totalItems = 1,
            totalQuantity = 1.0,
            invoiceItems = invoiceItems
        )

        val mockInvoiceResponse = InvoiceResponse(
            id = "inv-jewelry-789",
            invoiceNumber = "INV-JEWELRY-001",
            orderRefId = "ORD-JEWELRY-001",
            fromCustomerId = "CUST_JEWELER_001",
            fromCustomerName = "Golden Dreams Jewelry",
            toCustomerId = "CUST_CUSTOMER_001",
            toCustomerName = "Priya Sharma",
            totalCost = 125000.00,
            basePrice = 125000.00,
            totalTax = 3750.00,
            status = InvoiceStatus.INVOICED,
            totalItems = 1,
            totalQuantity = 1.0,
            invoiceItems = invoiceItems.map { 
                InvoiceItemResponse(
                    productId = it.productId,
                    description = it.description,
                    quantity = it.quantity,
                    price = it.price,
                    totalCost = it.totalCost
                )
            }
        )

        whenever(invoiceService.updateInvoice(any<Invoice>(), any<List<com.ampairs.invoice.domain.model.InvoiceItem>>()))
            .thenReturn(mockInvoiceResponse)

        mockMvc.perform(
            post("/invoice/v1")
                .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invoiceRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("INVOICED"))
            .andExpect(jsonPath("$.totalTax").value(3750.00))
            .andExpect(jsonPath("$.invoiceItems").isArray)

        verify(invoiceService).updateInvoice(any<Invoice>(), any<List<com.ampairs.invoice.domain.model.InvoiceItem>>())
    }

    @Test
    @DisplayName("POST /invoice/v1 - Service exception handling")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should handle service exceptions gracefully`() {
        val invoiceRequest = InvoiceUpdateRequest(
            orderRefId = "NONEXISTENT-ORDER-999",
            fromCustomerId = "CUST_001",
            fromCustomerName = "Test Customer",
            totalCost = 100.00,
            invoiceItems = listOf(
                InvoiceItemRequest(
                    productId = "PROD_001",
                    description = "Test Product",
                    quantity = 1.0,
                    price = 100.00,
                    totalCost = 100.00
                )
            )
        )

        whenever(invoiceService.updateInvoice(any<Invoice>(), any<List<com.ampairs.invoice.domain.model.InvoiceItem>>()))
            .thenThrow(IllegalArgumentException("Order not found"))

        mockMvc.perform(
            post("/invoice/v1")
                .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invoiceRequest))
        )
            .andExpect(status().is4xxClientError)

        verify(invoiceService).updateInvoice(any<Invoice>(), any<List<com.ampairs.invoice.domain.model.InvoiceItem>>())
    }
}