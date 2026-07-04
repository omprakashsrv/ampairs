package com.ampairs.product.service

import com.ampairs.product.domain.dto.ProductVariantResponse
import com.ampairs.product.domain.dto.asResponse
import com.ampairs.product.domain.dto.group.ProductBrandRequest
import com.ampairs.product.domain.dto.group.ProductCategoryRequest
import com.ampairs.product.domain.dto.group.ProductClassificationType
import com.ampairs.product.domain.dto.group.ProductGroupRequest
import com.ampairs.product.domain.dto.group.ProductSubCategoryRequest
import com.ampairs.product.domain.dto.group.asDatabaseModel
import com.ampairs.product.domain.dto.product.ProductRequest
import com.ampairs.product.domain.dto.product.asDatabaseModel as productAsDatabaseModel
import com.ampairs.product.domain.enums.TaxSpec
import com.ampairs.product.domain.enums.TaxType
import com.ampairs.product.domain.model.ProductType
import com.ampairs.product.domain.model.ProductVariant
import com.ampairs.product.domain.model.ServiceType
import com.ampairs.product.exception.DuplicateProductException
import com.ampairs.product.exception.InsufficientInventoryException
import com.ampairs.product.exception.InvalidPriceException
import com.ampairs.product.exception.InvalidProductDataException
import com.ampairs.product.exception.InventoryUpdateException
import com.ampairs.product.exception.ProductCategoryNotFoundException
import com.ampairs.product.exception.ProductExceptionHandler
import com.ampairs.product.exception.ProductNotFoundException
import com.ampairs.product.repository.ProductBrandRepository
import com.ampairs.product.repository.ProductCategoryRepository
import com.ampairs.product.repository.ProductGroupRepository
import com.ampairs.product.repository.ProductRepository
import com.ampairs.product.repository.ProductStandardCostRepository
import com.ampairs.product.repository.ProductSubCategoryRepository
import com.ampairs.product.sync.ProductCheckpointContributor
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductMiscUnitTest {

    // ---------- Request -> database model mappers ----------

    @Test
    fun `ProductRequest asDatabaseModel applies defaults and blank handling`() {
        val products = listOf(
            ProductRequest(id = "PRD-1", name = "Widget", sku = "  ", groupId = "", categoryId = "C1"),
        ).productAsDatabaseModel()

        val p = products.single()
        assertEquals("PRD-1", p.uid)
        assertEquals("Widget", p.name)
        assertNull(p.sku) // blank sku -> null
        assertEquals("ACTIVE", p.status) // default
        assertEquals("", p.taxCode) // default
        assertEquals(0.0, p.basePrice) // default
        assertNull(p.groupId) // blank -> null
        assertEquals("C1", p.categoryId)
        assertTrue(p.attributes.isEmpty())
    }

    @Test
    fun `ProductRequest asDatabaseModel keeps explicit values`() {
        val p = listOf(
            ProductRequest(
                id = "PRD-2", name = "Gadget", sku = "SKU-X", status = "INACTIVE", taxCode = "GST",
                basePrice = 5.0, mrp = 10.0, dp = 8.0, sellingPrice = 9.0,
                attributes = mapOf("a" to 1), groupId = "G1",
            ),
        ).productAsDatabaseModel().single()

        assertEquals("SKU-X", p.sku)
        assertEquals("INACTIVE", p.status)
        assertEquals("GST", p.taxCode)
        assertEquals(5.0, p.basePrice)
        assertEquals(10.0, p.mrp)
        assertEquals(8.0, p.dp)
        assertEquals(9.0, p.sellingPrice)
        assertEquals("G1", p.groupId)
        assertEquals(mapOf("a" to 1), p.attributes)
    }

    @Test
    fun `ProductGroupRequest asDatabaseModel maps fields`() {
        val g = listOf(
            ProductGroupRequest(id = "G1", name = "Tools", refId = "R", active = true, softDeleted = false, imageId = "IMG"),
        ).asDatabaseModel().single()
        assertEquals("G1", g.uid)
        assertEquals("Tools", g.name)
        assertEquals("R", g.refId)
        assertEquals("IMG", g.imageId)
    }

    @Test
    fun `ProductBrandRequest asDatabaseModel maps fields`() {
        val b = listOf(
            ProductBrandRequest(id = "B1", name = "Acme", refId = null, active = null, softDeleted = null, imageId = null),
        ).asDatabaseModel().single()
        assertEquals("B1", b.uid)
        assertEquals("Acme", b.name)
        assertNull(b.refId)
    }

    @Test
    fun `ProductCategoryRequest asDatabaseModel maps fields`() {
        val c = listOf(
            ProductCategoryRequest(id = "C1", name = "Cat", refId = "RC", active = true, softDeleted = false, imageId = null),
        ).asDatabaseModel().single()
        assertEquals("C1", c.uid)
        assertEquals("Cat", c.name)
        assertEquals("RC", c.refId)
    }

    @Test
    fun `ProductSubCategoryRequest asDatabaseModel maps fields`() {
        val s = listOf(
            ProductSubCategoryRequest(id = "S1", name = "Sub", refId = "RS", active = true, softDeleted = false, imageId = "I"),
        ).asDatabaseModel().single()
        assertEquals("S1", s.uid)
        assertEquals("Sub", s.name)
        assertEquals("I", s.imageId)
    }

    // ---------- ProductVariant computed properties + DTO ----------

    private fun variant(block: ProductVariant.() -> Unit = {}) = ProductVariant().apply {
        uid = "VAR-1"
        productId = "PRD-1"
        sku = "SKU-V1"
        variantName = "Variant"
        stockQuantity = BigDecimal("10")
        createdAt = Instant.EPOCH
        updatedAt = Instant.EPOCH
        block()
    }

    @Test
    fun `variant displayName joins attribute values`() {
        val v = variant {
            attribute1Value = "Red"
            attribute2Value = "Large"
            attribute3Value = "Cotton"
        }
        assertEquals("Red - Large - Cotton", v.displayName)
    }

    @Test
    fun `variant displayName falls back to variantName when no attributes`() {
        assertEquals("Variant", variant().displayName)
    }

    @Test
    fun `variant isLowStock true when at or below alert`() {
        val v = variant { stockQuantity = BigDecimal("3"); lowStockAlert = BigDecimal("5") }
        assertTrue(v.isLowStock)
    }

    @Test
    fun `variant isLowStock false when above alert`() {
        val v = variant { stockQuantity = BigDecimal("9"); lowStockAlert = BigDecimal("5") }
        assertFalse(v.isLowStock)
    }

    @Test
    fun `variant isLowStock false when no alert configured`() {
        assertFalse(variant { lowStockAlert = null }.isLowStock)
    }

    @Test
    fun `ProductVariant asResponse maps all fields`() {
        val v = variant {
            attribute1Name = "Color"; attribute1Value = "Red"
            mrp = BigDecimal("100"); sellingPrice = BigDecimal("90"); dp = BigDecimal("80")
            lowStockAlert = BigDecimal("2"); active = true; synced = true
        }
        val r: ProductVariantResponse = v.asResponse()
        assertEquals("VAR-1", r.uid)
        assertEquals("PRD-1", r.productId)
        assertEquals("SKU-V1", r.sku)
        assertEquals("Red", r.attribute1Value)
        assertEquals("Color", r.attribute1Name)
        assertEquals(BigDecimal("100"), r.mrp)
        assertEquals(BigDecimal("90"), r.sellingPrice)
        assertEquals("Red", r.displayName)
        assertFalse(r.isLowStock) // stock 10 > alert 2
        assertTrue(r.active)
        assertTrue(r.synced)
    }

    @Test
    fun `ProductVariant list asResponse maps each`() {
        val responses = listOf(variant(), variant { uid = "VAR-2" }).asResponse()
        assertEquals(2, responses.size)
        assertEquals("VAR-2", responses[1].uid)
    }

    // ---------- enums ----------

    @Test
    fun `TaxSpec carries display name and description`() {
        assertEquals("Interstate", TaxSpec.INTER.displayName)
        assertTrue(TaxSpec.INTRA.description.contains("SGST"))
        assertEquals(6, TaxSpec.entries.size)
    }

    @Test
    fun `TaxType has HSN and SAC`() {
        assertEquals(setOf("HSN", "SAC"), TaxType.entries.map { it.name }.toSet())
    }

    @Test
    fun `ProductType fromString is case-insensitive and null-safe`() {
        assertEquals(ProductType.RETAIL, ProductType.fromString("retail"))
        assertEquals(ProductType.SERVICE, ProductType.fromString("SERVICE"))
        assertNull(ProductType.fromString("unknown"))
        assertNull(ProductType.fromString(null))
    }

    @Test
    fun `ServiceType fromString is case-insensitive and null-safe`() {
        assertEquals(ServiceType.DIGITAL, ServiceType.fromString("digital"))
        assertNull(ServiceType.fromString("bad"))
        assertNull(ServiceType.fromString(null))
    }

    @Test
    fun `ProductClassificationType has four values`() {
        assertEquals(
            setOf("BRAND", "CATEGORY", "GROUP", "SUB_CATEGORY"),
            ProductClassificationType.entries.map { it.name }.toSet(),
        )
    }

    // ---------- ProductCheckpointContributor ----------

    @Mock private lateinit var productRepository: ProductRepository
    @Mock private lateinit var productCategoryRepository: ProductCategoryRepository
    @Mock private lateinit var productBrandRepository: ProductBrandRepository
    @Mock private lateinit var productGroupRepository: ProductGroupRepository
    @Mock private lateinit var productSubCategoryRepository: ProductSubCategoryRepository
    @Mock private lateinit var productStandardCostRepository: ProductStandardCostRepository

    private lateinit var checkpointContributor: ProductCheckpointContributor

    @BeforeEach
    fun setUp() {
        checkpointContributor = ProductCheckpointContributor(
            productRepository, productCategoryRepository, productBrandRepository,
            productGroupRepository, productSubCategoryRepository, productStandardCostRepository,
        )
    }

    @Test
    fun `checkpoints returns product and aggregated catalog max`() {
        whenever(productRepository.findMaxUpdatedAt()).thenReturn(Instant.parse("2025-03-01T00:00:00Z"))
        whenever(productCategoryRepository.findMaxUpdatedAt()).thenReturn(Instant.parse("2025-01-01T00:00:00Z"))
        whenever(productBrandRepository.findMaxUpdatedAt()).thenReturn(Instant.parse("2025-05-01T00:00:00Z"))
        whenever(productGroupRepository.findMaxUpdatedAt()).thenReturn(null)
        whenever(productSubCategoryRepository.findMaxUpdatedAt()).thenReturn(Instant.parse("2025-02-01T00:00:00Z"))
        whenever(productStandardCostRepository.findMaxUpdatedAt()).thenReturn(Instant.parse("2025-04-01T00:00:00Z"))

        val checkpoints = checkpointContributor.checkpoints()

        assertEquals(Instant.parse("2025-03-01T00:00:00Z"), checkpoints["product"])
        // max across catalog = brand (2025-05-01)
        assertEquals(Instant.parse("2025-05-01T00:00:00Z"), checkpoints["product_catalog"])
        assertEquals(Instant.parse("2025-04-01T00:00:00Z"), checkpoints["product_standard_cost"])
    }

    @Test
    fun `checkpoints catalog is null when all catalog tables empty`() {
        whenever(productRepository.findMaxUpdatedAt()).thenReturn(null)
        whenever(productCategoryRepository.findMaxUpdatedAt()).thenReturn(null)
        whenever(productBrandRepository.findMaxUpdatedAt()).thenReturn(null)
        whenever(productGroupRepository.findMaxUpdatedAt()).thenReturn(null)
        whenever(productSubCategoryRepository.findMaxUpdatedAt()).thenReturn(null)

        val checkpoints = checkpointContributor.checkpoints()

        assertNull(checkpoints["product"])
        assertNull(checkpoints["product_catalog"])
    }

    // ---------- ProductExceptionHandler ----------

    private val handler = ProductExceptionHandler()
    private val request: HttpServletRequest = mock { whenever(it.requestURI).thenReturn("/api/product/v1/x") }

    @Test
    fun `handleProductNotFound returns 404`() {
        val r = handler.handleProductNotFoundException(ProductNotFoundException("nope"), request)
        assertEquals(HttpStatus.NOT_FOUND, r.statusCode)
        assertFalse(r.body!!.success)
    }

    @Test
    fun `handleInsufficientInventory returns 409`() {
        val r = handler.handleInsufficientInventoryException(InsufficientInventoryException("low"), request)
        assertEquals(HttpStatus.CONFLICT, r.statusCode)
    }

    @Test
    fun `handleDuplicateProduct returns 409`() {
        val r = handler.handleDuplicateProductException(DuplicateProductException("dup"), request)
        assertEquals(HttpStatus.CONFLICT, r.statusCode)
    }

    @Test
    fun `handleInvalidProductData returns 400`() {
        val r = handler.handleInvalidProductDataException(InvalidProductDataException("bad"), request)
        assertEquals(HttpStatus.BAD_REQUEST, r.statusCode)
    }

    @Test
    fun `handleProductCategoryNotFound returns 404`() {
        val r = handler.handleProductCategoryNotFoundException(ProductCategoryNotFoundException("nope"), request)
        assertEquals(HttpStatus.NOT_FOUND, r.statusCode)
    }

    @Test
    fun `handleInventoryUpdate returns 500`() {
        val r = handler.handleInventoryUpdateException(InventoryUpdateException("fail"), request)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.statusCode)
    }

    @Test
    fun `handleInvalidPrice returns 400`() {
        val r = handler.handleInvalidPriceException(InvalidPriceException("bad price"), request)
        assertEquals(HttpStatus.BAD_REQUEST, r.statusCode)
    }

    @Test
    fun `exception classes carry message and cause`() {
        val cause = RuntimeException("root")
        val ex = ProductNotFoundException("msg", cause)
        assertEquals("msg", ex.message)
        assertEquals(cause, ex.cause)
    }
}
