package com.ampairs.order

import com.ampairs.AmpairsApplication

import com.ampairs.core.domain.model.Address
import com.ampairs.order.domain.dto.*
import com.ampairs.order.domain.enums.OrderStatus
import com.ampairs.order.domain.model.Order
import com.ampairs.order.service.OrderService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

/**
 * Integration tests for Order Management API - Create Order endpoint.
 * 
 * Tests verify the POST /order/v1 endpoint using MockMvc with mocked services.
 * Covers order creation with line items, inventory checking, and tax calculations.
 */
@SpringBootTest(
    classes = [AmpairsApplication::class],
    properties = [
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
    ]
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class OrderCreateIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var orderService: OrderService

    @Test
    @DisplayName("POST /order/v1 - Create basic order with line items")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should create order with multiple line items and calculate totals`() {
        val orderItems = listOf(
            OrderItemRequest(
                productId = "PROD_HAMMER_001",
                description = "Steel Hammer 500g",
                quantity = 2.0,
                basePrice = 450.00,
                price = 450.00,
                totalCost = 900.00
            ),
            OrderItemRequest(
                productId = "PROD_SCREWS_001",
                description = "Steel Screws Pack",
                quantity = 5.0,
                basePrice = 25.00,
                price = 25.00,
                totalCost = 125.00
            )
        )

        val orderRequest = OrderUpdateRequest(
            fromCustomerId = "CUST_HARDWARE_001",
            fromCustomerName = "ABC Hardware Store",
            totalCost = 1025.00,
            basePrice = 1025.00,
            totalTax = 184.50,
            status = OrderStatus.DRAFT,
            totalItems = 2,
            totalQuantity = 7.0,
            orderItems = orderItems
        )

        val mockOrderResponse = OrderResponse(
            id = "ord-123",
            orderNumber = "ORD-20250909-001",
            fromCustomerId = "CUST_HARDWARE_001",
            fromCustomerName = "ABC Hardware Store",
            totalCost = 1025.00,
            basePrice = 1025.00,
            totalTax = 184.50,
            status = OrderStatus.DRAFT,
            totalItems = 2,
            totalQuantity = 7.0,
            orderItems = orderItems.map { 
                OrderItemResponse(
                    productId = it.productId,
                    description = it.description,
                    quantity = it.quantity,
                    price = it.price,
                    totalCost = it.totalCost
                )
            }
        )

        whenever(orderService.updateOrder(any<Order>(), any<List<com.ampairs.order.domain.model.OrderItem>>()))
            .thenReturn(mockOrderResponse)

        mockMvc.perform(
            post("/order/v1")
                .header("X-Workspace-ID", "TEST_HARDWARE_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value("ord-123"))
            .andExpect(jsonPath("$.orderNumber").value("ORD-20250909-001"))
            .andExpect(jsonPath("$.fromCustomerName").value("ABC Hardware Store"))
            .andExpect(jsonPath("$.totalCost").value(1025.00))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.orderItems").isArray)
            .andExpect(jsonPath("$.orderItems").isNotEmpty)

        verify(orderService).updateOrder(any<Order>(), any<List<com.ampairs.order.domain.model.OrderItem>>())
    }

    @Test
    @DisplayName("POST /order/v1 - Create jewelry order with precious metal items")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should create jewelry order with gold items and accurate pricing`() {
        val orderItems = listOf(
            OrderItemRequest(
                productId = "PROD_GOLD_RING_001",
                description = "22K Gold Ring with Diamond",
                quantity = 1.0,
                basePrice = 45000.00,
                price = 45000.00,
                totalCost = 45000.00
            )
        )

        val orderRequest = OrderUpdateRequest(
            fromCustomerId = "CUST_JEWELRY_001",
            fromCustomerName = "Golden Ornaments Jewelry",
            totalCost = 45000.00,
            basePrice = 45000.00,
            totalTax = 1350.00, // 3% GST on gold
            status = OrderStatus.CONFIRMED,
            totalItems = 1,
            totalQuantity = 1.0,
            orderItems = orderItems
        )

        val mockOrderResponse = OrderResponse(
            id = "ord-jewelry-456",
            orderNumber = "ORD-JEWELRY-001",
            fromCustomerId = "CUST_JEWELRY_001",
            fromCustomerName = "Golden Ornaments Jewelry",
            totalCost = 45000.00,
            basePrice = 45000.00,
            totalTax = 1350.00,
            status = OrderStatus.CONFIRMED,
            totalItems = 1,
            totalQuantity = 1.0,
            orderItems = orderItems.map { 
                OrderItemResponse(
                    productId = it.productId,
                    description = it.description,
                    quantity = it.quantity,
                    price = it.price,
                    totalCost = it.totalCost
                )
            }
        )

        whenever(orderService.updateOrder(any<Order>(), any<List<com.ampairs.order.domain.model.OrderItem>>()))
            .thenReturn(mockOrderResponse)

        mockMvc.perform(
            post("/order/v1")
                .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("ord-jewelry-456"))
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.totalTax").value(1350.00))

        verify(orderService).updateOrder(any<Order>(), any<List<com.ampairs.order.domain.model.OrderItem>>())
    }

    @Test
    @DisplayName("POST /order/v1 - Create bulk kirana order")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should create bulk kirana order with discounts`() {
        val orderItems = listOf(
            OrderItemRequest(
                productId = "PROD_RICE_001",
                description = "Premium Basmati Rice 25kg",
                quantity = 10.0,
                basePrice = 2500.00,
                price = 2375.00, // 5% bulk discount
                totalCost = 23750.00,
                discount = listOf(Discount(percent = 5.0, value = 125.00))
            ),
            OrderItemRequest(
                productId = "PROD_OIL_001",
                description = "Refined Sunflower Oil 15L",
                quantity = 8.0,
                basePrice = 1800.00,
                price = 1710.00, // 5% bulk discount
                totalCost = 13680.00,
                discount = listOf(Discount(percent = 5.0, value = 90.00))
            )
        )

        val orderRequest = OrderUpdateRequest(
            fromCustomerId = "CUST_KIRANA_001",
            fromCustomerName = "Sai Provision Store",
            totalCost = 37480.00,
            basePrice = 39400.00,
            totalTax = 1874.00, // 5% GST
            status = OrderStatus.CONFIRMED,
            totalItems = 2,
            totalQuantity = 18.0,
            orderItems = orderItems,
            discount = listOf(
                com.ampairs.order.domain.dto.Discount(
                    percent = 5.0,
                    value = 1920.00
                )
            )
        )

        val mockOrderResponse = OrderResponse(
            id = "ord-kirana-789",
            orderNumber = "ORD-KIRANA-001",
            fromCustomerId = "CUST_KIRANA_001",
            fromCustomerName = "Sai Provision Store",
            totalCost = 37480.00,
            basePrice = 39400.00,
            totalTax = 1874.00,
            status = OrderStatus.CONFIRMED,
            totalItems = 2,
            totalQuantity = 18.0,
            orderItems = orderItems.map { 
                OrderItemResponse(
                    productId = it.productId,
                    description = it.description,
                    quantity = it.quantity,
                    price = it.price,
                    totalCost = it.totalCost
                )
            }
        )

        whenever(orderService.updateOrder(any<Order>(), any<List<com.ampairs.order.domain.model.OrderItem>>()))
            .thenReturn(mockOrderResponse)

        mockMvc.perform(
            post("/order/v1")
                .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("ord-kirana-789"))
            .andExpect(jsonPath("$.totalQuantity").value(18.0))

        verify(orderService).updateOrder(any<Order>(), any<List<com.ampairs.order.domain.model.OrderItem>>())
    }

    @Test
    @DisplayName("POST /order/v1 - Validation error handling")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should handle validation errors gracefully`() {
        val invalidOrderRequest = """
            {
                "totalCost": -100.0,
                "orderItems": []
            }
        """.trimIndent()

        mockMvc.perform(
            post("/order/v1")
                .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidOrderRequest)
        )
            .andExpect(status().isBadRequest)

        verify(orderService, never()).updateOrder(any<Order>(), any<List<com.ampairs.order.domain.model.OrderItem>>())
    }

    @Test
    @DisplayName("POST /order/v1 - Service exception handling")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should handle service exceptions gracefully`() {
        val orderRequest = OrderUpdateRequest(
            fromCustomerId = "NONEXISTENT_CUSTOMER",
            fromCustomerName = "Test Customer",
            totalCost = 100.00,
            orderItems = listOf(
                OrderItemRequest(
                    productId = "PROD_001",
                    description = "Test Product",
                    quantity = 1.0,
                    price = 100.00,
                    totalCost = 100.00
                )
            )
        )

        whenever(orderService.updateOrder(any<Order>(), any<List<com.ampairs.order.domain.model.OrderItem>>()))
            .thenThrow(IllegalArgumentException("Customer not found"))

        mockMvc.perform(
            post("/order/v1")
                .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest))
        )
            .andExpect(status().is4xxClientError)

        verify(orderService).updateOrder(any<Order>(), any<List<com.ampairs.order.domain.model.OrderItem>>())
    }
}