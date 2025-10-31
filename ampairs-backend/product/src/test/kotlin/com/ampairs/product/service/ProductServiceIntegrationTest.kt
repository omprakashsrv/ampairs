package com.ampairs.product.service

import com.ampairs.event.domain.events.ProductCreatedEvent
import com.ampairs.product.domain.model.Product
import com.ampairs.product.repository.*
import com.ampairs.unit.domain.dto.UnitResponse
import com.ampairs.unit.service.UnitService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Optional

@SpringBootTest(
    classes = [
        ProductService::class,
        ProductServiceIntegrationTest.TestConfig::class
    ]
)
class ProductServiceIntegrationTest(
    private val productService: ProductService,
    private val unitService: UnitService,
    private val productRepository: ProductRepository,
    private val eventPublisher: ApplicationEventPublisher
) {

    @Test
    fun `should create product with unit from unit module`() {
        val unitUid = "UNIT-10"
        val unitResponse = UnitResponse(
            uid = unitUid,
            name = "Kilogram",
            shortName = "kg",
            decimalPlaces = 3,
            refId = null,
            active = true,
            createdAt = null,
            updatedAt = null
        )

        whenever(unitService.findByUid(unitUid)).thenReturn(unitResponse)
        whenever(productRepository.findBySku("SKU-123")).thenReturn(Optional.empty())
        whenever(productRepository.save(any())).thenAnswer { invocation ->
            invocation.getArgument<Product>(0)
        }

        val product = Product().apply {
            name = "Rice Bag"
            sku = "SKU-123"
            unitId = unitUid
        }

        val savedProduct = productService.createProduct(product)

        assertNotNull(savedProduct)
        assertEquals(unitUid, savedProduct.unitId)
        verify(unitService).findByUid(unitUid)
        verify(productRepository).save(any())
        verify(eventPublisher).publishEvent(any<ProductCreatedEvent>())
    }

    @Configuration
    class TestConfig {
        @Bean
        fun productPagingRepository(): ProductPagingRepository = mock()

        @Bean
        fun unitService(): UnitService = mock()

        @Bean
        fun productGroupRepository(): ProductGroupRepository = mock()

        @Bean
        fun productBrandRepository(): ProductBrandRepository = mock()

        @Bean
        fun productCategoryRepository(): ProductCategoryRepository = mock()

        @Bean
        fun productSubCategoryRepository(): ProductSubCategoryRepository = mock()

        @Bean
        fun productRepository(): ProductRepository = mock()

        @Bean
        fun eventPublisher(): ApplicationEventPublisher = mock()
    }
}
