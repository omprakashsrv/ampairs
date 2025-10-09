package com.ampairs.product

import com.ampairs.AmpairsApplication

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.product.domain.dto.product.ProductRequest
import com.ampairs.product.domain.dto.product.ProductResponse
import com.ampairs.product.domain.model.Product
import com.ampairs.product.service.ProductService
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
 * Integration tests for Product Management API - Create Product endpoint.
 * 
 * Tests verify the POST /product/v1 endpoint using MockMvc with mocked services.
 * Covers product creation with retail business-specific fields and attributes.
 */
@SpringBootTest(classes = [AmpairsApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductCreateIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var productService: ProductService

    @Test
    @DisplayName("POST /product/v1 - Create basic retail product")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should create basic retail product with required fields`() {
        val productRequest = ProductRequest(
            name = "Steel Hammer 500g",
            sku = "HAM-ST-500",
            description = "Heavy duty steel hammer for construction",
            unitId = "unit-pieces",
            taxCodeId = "tax-gst-18",
            basePrice = 450.00,
            costPrice = 300.00
        )

        val mockProduct = Product().apply {
            uid = "prod-123"
            name = "Steel Hammer 500g"
            sku = "HAM-ST-500"
            description = "Heavy duty steel hammer for construction"
            unitId = "unit-pieces"
            taxCodeId = "tax-gst-18"
            basePrice = 450.00
            costPrice = 300.00
            status = "ACTIVE"
            createdAt = LocalDateTime.now()
            updatedAt = LocalDateTime.now()
        }

        whenever(productService.createProduct(any<Product>()))
            .thenReturn(mockProduct)

        mockMvc.perform(
            post("/product/v1")
                .header("X-Workspace-ID", "TEST_HARDWARE_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.sku").value("HAM-ST-500"))
            .andExpect(jsonPath("$.data.name").value("Steel Hammer 500g"))

        verify(productService).createProduct(any<Product>())
    }

    @Test
    @DisplayName("POST /product/v1 - Create jewelry product with weight attributes")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should create jewelry product with precious metal attributes`() {
        val productRequest = ProductRequest(
            name = "Gold Ring 22K",
            sku = "RING-GOLD-22K-001",
            description = "22 karat gold ring with diamond setting",
            unitId = "unit-grams",
            taxCodeId = "tax-gst-3",
            basePrice = 8500.00,
            costPrice = 7200.00,
            attributes = mapOf(
                "weightGrams" to 5.2,
                "purity" to "22K",
                "metalType" to "GOLD",
                "stoneType" to "DIAMOND",
                "stoneWeightCarats" to 0.25,
                "certification" to "BIS_HALLMARK"
            )
        )

        val mockProduct = Product().apply {
            uid = "prod-456"
            name = "Gold Ring 22K"
            sku = "RING-GOLD-22K-001"
            description = "22 karat gold ring with diamond setting"
            unitId = "unit-grams"
            taxCodeId = "tax-gst-3"
            basePrice = 8500.00
            costPrice = 7200.00
            status = "ACTIVE"
            attributes = mapOf(
                "weightGrams" to 5.2,
                "purity" to "22K",
                "metalType" to "GOLD",
                "stoneType" to "DIAMOND",
                "stoneWeightCarats" to 0.25,
                "certification" to "BIS_HALLMARK"
            )
            createdAt = LocalDateTime.now()
            updatedAt = LocalDateTime.now()
        }

        whenever(productService.createProduct(any<Product>()))
            .thenReturn(mockProduct)

        mockMvc.perform(
            post("/product/v1")
                .header("X-Workspace-ID", "TEST_JEWELRY_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Gold Ring 22K"))
            .andExpect(jsonPath("$.data.sku").value("RING-GOLD-22K-001"))

        verify(productService).createProduct(any<Product>())
    }

    @Test
    @DisplayName("POST /product/v1 - Validation error for missing required fields")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should return validation error when required fields are missing`() {
        val invalidProductRequest = """
            {
                "description": "Incomplete product",
                "base_price": 100.00
            }
        """.trimIndent()

        mockMvc.perform(
            post("/product/v1")
                .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidProductRequest)
        )
            .andExpect(status().isBadRequest)

        verify(productService, never()).createProduct(any<Product>())
    }

    @Test
    @DisplayName("POST /product/v1 - Service exception handling")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should handle service exceptions gracefully`() {
        val productRequest = ProductRequest(
            name = "Test Product",
            sku = "TEST-SKU-001"
        )

        whenever(productService.createProduct(any<Product>()))
            .thenThrow(IllegalArgumentException("SKU already exists: TEST-SKU-001"))

        mockMvc.perform(
            post("/product/v1")
                .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productRequest))
        )
            .andExpect(status().is4xxClientError)

        verify(productService).createProduct(any<Product>())
    }

    @Test
    @DisplayName("POST /product/v1 - Create kirana product with bulk attributes")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should create kirana product with bulk quantity pricing`() {
        val productRequest = ProductRequest(
            name = "Rice Premium Basmati",
            sku = "RICE-BASMATI-25KG",
            description = "Premium quality basmati rice 25kg bag",
            unitId = "unit-kg",
            taxCodeId = "tax-gst-5",
            basePrice = 2500.00,
            costPrice = 2200.00,
            attributes = mapOf(
                "bulkDiscount" to mapOf(
                    "minQuantity" to 10,
                    "discountPercent" to 5.0
                ),
                "storageRequirement" to "DRY_PLACE",
                "expiryMonths" to 12,
                "brand" to "Premium Select"
            )
        )

        val mockProduct = Product().apply {
            uid = "prod-789"
            name = "Rice Premium Basmati"
            sku = "RICE-BASMATI-25KG"
            description = "Premium quality basmati rice 25kg bag"
            unitId = "unit-kg"
            taxCodeId = "tax-gst-5"
            basePrice = 2500.00
            costPrice = 2200.00
            status = "ACTIVE"
            attributes = mapOf(
                "bulkDiscount" to mapOf(
                    "minQuantity" to 10,
                    "discountPercent" to 5.0
                ),
                "storageRequirement" to "DRY_PLACE",
                "expiryMonths" to 12,
                "brand" to "Premium Select"
            )
            createdAt = LocalDateTime.now()
            updatedAt = LocalDateTime.now()
        }

        whenever(productService.createProduct(any<Product>()))
            .thenReturn(mockProduct)

        mockMvc.perform(
            post("/product/v1")
                .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Rice Premium Basmati"))
            .andExpect(jsonPath("$.data.sku").value("RICE-BASMATI-25KG"))

        verify(productService).createProduct(any<Product>())
    }
}