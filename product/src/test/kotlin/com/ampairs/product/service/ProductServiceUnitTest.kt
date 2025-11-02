package com.ampairs.product.service

import com.ampairs.event.domain.events.ProductCreatedEvent
import com.ampairs.product.domain.model.Product
import com.ampairs.product.repository.ProductBrandRepository
import com.ampairs.product.repository.ProductCategoryRepository
import com.ampairs.product.repository.ProductGroupRepository
import com.ampairs.product.repository.ProductPagingRepository
import com.ampairs.product.repository.ProductRepository
import com.ampairs.product.repository.ProductSubCategoryRepository
import com.ampairs.unit.domain.dto.UnitResponse
import com.ampairs.unit.service.UnitService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.context.ApplicationEventPublisher
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ProductServiceUnitTest {

    @Mock private lateinit var productPagingRepository: ProductPagingRepository
    @Mock private lateinit var unitService: UnitService
    @Mock private lateinit var productGroupRepository: ProductGroupRepository
    @Mock private lateinit var productBrandRepository: ProductBrandRepository
    @Mock private lateinit var productCategoryRepository: ProductCategoryRepository
    @Mock private lateinit var productSubCategoryRepository: ProductSubCategoryRepository
    @Mock private lateinit var productRepository: ProductRepository
    @Mock private lateinit var eventPublisher: ApplicationEventPublisher

    private lateinit var productService: ProductService

    @BeforeEach
    fun setup() {
        productService = ProductService(
            productPagingRepository = productPagingRepository,
            unitService = unitService,
            productGroupRepository = productGroupRepository,
            productBrandRepository = productBrandRepository,
            productCategoryRepository = productCategoryRepository,
            productSubCategoryRepository = productSubCategoryRepository,
            productRepository = productRepository,
            eventPublisher = eventPublisher
        )
    }

    @Test
    fun `create product should validate unit`() {
        val product = Product().apply {
            name = "Test Product"
            unitId = "UNIT-10"
            sku = "SKU-123"
        }

        whenever(unitService.findByUid("UNIT-10")).thenReturn(
            UnitResponse(
                uid = "UNIT-10",
                name = "Kilogram",
                shortName = "kg",
                decimalPlaces = 3,
                refId = null,
                active = true,
                createdAt = null,
                updatedAt = null
            )
        )
        whenever(productRepository.findBySku("SKU-123")).thenReturn(Optional.empty())
        whenever(productRepository.save(product)).thenReturn(product)

        productService.createProduct(product)

        verify(unitService).findByUid("UNIT-10")
        verify(productRepository).save(product)
        verify(eventPublisher).publishEvent(any<ProductCreatedEvent>())
    }

    @Test
    fun `create product should throw when unit missing`() {
        val product = Product().apply {
            name = "Test Product"
            unitId = "UNIT-MISSING"
            sku = "SKU-456"
        }

        whenever(unitService.findByUid("UNIT-MISSING")).thenReturn(null)

        assertThrows<IllegalArgumentException> {
            productService.createProduct(product)
        }
    }
}
