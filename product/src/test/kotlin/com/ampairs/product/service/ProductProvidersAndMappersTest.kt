package com.ampairs.product.service

import com.ampairs.form.domain.model.EntityType
import com.ampairs.form.domain.model.FieldDataType
import com.ampairs.form.domain.model.OptionSource
import com.ampairs.product.domain.dto.group.asResponse as groupAsResponse
import com.ampairs.product.domain.dto.product.asResponse
import com.ampairs.product.domain.model.Product
import com.ampairs.product.domain.model.ProductType
import com.ampairs.product.domain.model.ServiceType
import com.ampairs.product.domain.model.group.ProductBrand
import com.ampairs.product.domain.model.group.ProductCategory
import com.ampairs.product.domain.model.group.ProductGroup
import com.ampairs.product.domain.model.group.ProductSubCategory
import com.ampairs.product.repository.ProductRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.time.Instant

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductProvidersAndMappersTest {

    // ---------- ProductStandardFieldProvider ----------

    private val fieldProvider = ProductStandardFieldProvider()

    @Test
    fun `field provider declares PRODUCT entity type`() {
        assertEquals(EntityType.PRODUCT, fieldProvider.entityType())
    }

    @Test
    fun `field provider declares the four sections in order`() {
        val sections = fieldProvider.sections()
        assertEquals(listOf("Basic Info", "Classification", "Pricing", "Tax & Units"), sections.map { it.name })
        assertEquals(listOf(0, 1, 2, 3), sections.map { it.order })
    }

    @Test
    fun `field provider name field is essential and mandatory with validation`() {
        val name = fieldProvider.standardFields().first { it.key == "name" }
        assertTrue(name.essential)
        assertTrue(name.mandatory)
        assertEquals(FieldDataType.TEXT, name.dataType)
        assertEquals("Basic Info", name.section)
        assertTrue(name.validationRules.isNotEmpty())
    }

    @Test
    fun `field provider status field is static choice with defaults`() {
        val status = fieldProvider.standardFields().first { it.key == "status" }
        assertEquals(FieldDataType.CHOICE, status.dataType)
        assertEquals(OptionSource.STATIC, status.optionSource)
        assertEquals(listOf("ACTIVE", "INACTIVE"), status.enumValues)
        assertEquals("ACTIVE", status.defaultValue)
    }

    @Test
    fun `field provider dynamic choice fields reference correct source keys`() {
        val byKey = fieldProvider.standardFields().associateBy { it.key }
        assertEquals("product_groups", byKey["groupId"]!!.dynamicSourceKey)
        assertEquals("product_categories", byKey["categoryId"]!!.dynamicSourceKey)
        assertEquals("product_sub_categories", byKey["subCategoryId"]!!.dynamicSourceKey)
        assertEquals("product_brands", byKey["brandId"]!!.dynamicSourceKey)
        assertEquals("tax_codes", byKey["taxCodeId"]!!.dynamicSourceKey)
        assertEquals("units", byKey["baseUnitId"]!!.dynamicSourceKey)
        assertTrue(byKey.values.filter { it.optionSource == OptionSource.DYNAMIC }.all { it.dynamicSourceKey != null })
    }

    @Test
    fun `field provider exposes all expected field keys`() {
        val keys = fieldProvider.standardFields().map { it.key }.toSet()
        assertEquals(
            setOf("name", "code", "sku", "description", "status", "groupId", "categoryId",
                "subCategoryId", "brandId", "basePrice", "costPrice", "mrp", "sellingPrice",
                "taxCodeId", "baseUnitId"),
            keys,
        )
    }

    // ---------- UnitProductUsageProvider ----------

    @Mock private lateinit var productRepository: ProductRepository
    private lateinit var usageProvider: UnitProductUsageProvider

    @BeforeEach
    fun setUp() {
        usageProvider = UnitProductUsageProvider(productRepository)
    }

    @Test
    fun `usage provider returns empty snapshot when no products use unit`() {
        whenever(productRepository.findAllByUnitId("UNIT-1")).thenReturn(emptyList())
        val snapshot = usageProvider.findUsage("UNIT-1")
        assertEquals("UNIT-1", snapshot.unitUid)
        assertFalse(snapshot.inUse)
        assertEquals(0, snapshot.entityCount)
        assertTrue(snapshot.entityIds.isEmpty())
    }

    @Test
    fun `usage provider returns entity ids when products use unit`() {
        val p1 = Product().apply { uid = "PRD-1" }
        val p2 = Product().apply { uid = "PRD-2" }
        whenever(productRepository.findAllByUnitId("UNIT-1")).thenReturn(listOf(p1, p2))
        val snapshot = usageProvider.findUsage("UNIT-1")
        assertEquals(listOf("PRD-1", "PRD-2"), snapshot.entityIds)
        assertTrue(snapshot.inUse)
        assertEquals(2, snapshot.entityCount)
    }

    // ---------- Product.asResponse mapper ----------

    @Test
    fun `Product asResponse maps scalar fields and nulls to empty strings`() {
        val product = Product().apply {
            uid = "PRD-9"
            refId = "REF-9"
            name = "Widget"
            code = "C"
            sku = "S"
            description = "desc"
            status = "ACTIVE"
            taxCode = "GST18"
            taxCodeId = "TX-1"
            unitId = "U-1"
            basePrice = 5.0
            costPrice = 3.0
            mrp = 10.0
            dp = 8.0
            sellingPrice = 9.0
            attributes = mapOf("k" to "v")
            createdAt = Instant.EPOCH
            updatedAt = Instant.EPOCH
            productType = ProductType.RETAIL
            serviceType = ServiceType.DIGITAL
            hasVariants = true
            isEcomListed = true
            // groupId/categoryId/etc left null
        }

        val response = product.asResponse()

        assertEquals("PRD-9", response.id)
        assertEquals("REF-9", response.refId)
        assertEquals("Widget", response.name)
        assertEquals("GST18", response.taxCode)
        assertEquals(5.0, response.basePrice)
        assertEquals(9.0, response.sellingPrice)
        assertEquals(mapOf("k" to "v"), response.attributes)
        // null ids become empty strings
        assertEquals("", response.groupId)
        assertEquals("", response.categoryId)
        assertEquals("", response.subCategoryId)
        assertEquals("", response.brandId)
        assertEquals("RETAIL", response.productType)
        assertEquals("DIGITAL", response.serviceType)
        assertTrue(response.hasVariants)
        assertTrue(response.isEcomListed)
        assertNull(response.inventory)
        assertTrue(response.images!!.isEmpty())
        assertNull(response.baseUnit)
    }

    @Test
    fun `Product list asResponse maps each element`() {
        val list = listOf(
            Product().apply { uid = "A"; name = "a"; groupId = "G1" },
            Product().apply { uid = "B"; name = "b" },
        )
        val responses = list.asResponse()
        assertEquals(2, responses.size)
        assertEquals("A", responses[0].id)
        assertEquals("G1", responses[0].groupId)
        assertEquals("", responses[1].groupId)
    }

    @Test
    fun `Product asResponse handles null productType and serviceType`() {
        val response = Product().apply { uid = "X"; name = "n" }.asResponse()
        assertNull(response.productType)
        assertNull(response.serviceType)
        assertFalse(response.hasVariants)
    }

    // ---------- group DTO mappers ----------

    @Test
    fun `ProductGroup asResponse maps fields`() {
        val group = ProductGroup().apply { uid = "G1"; name = "Tools"; refId = "R"; imageId = "IMG" }
        val response = group.groupAsResponse()
        assertEquals("G1", response.id)
        assertEquals("Tools", response.name)
        assertEquals("R", response.refId)
        assertEquals("IMG", response.imageId)
        assertNull(response.image)
    }

    @Test
    fun `ProductGroup list asResponse maps each`() {
        val responses = listOf(ProductGroup().apply { uid = "G1"; name = "a" }).groupAsResponse()
        assertEquals(1, responses.size)
        assertEquals("G1", responses.single().id)
    }

    @Test
    fun `ProductBrand asResponse maps fields`() {
        val response = ProductBrand().apply { uid = "B1"; name = "Acme"; refId = "RB" }.groupAsResponse()
        assertEquals("B1", response.id)
        assertEquals("Acme", response.name)
        assertEquals("RB", response.refId)
        assertNull(response.image)
    }

    @Test
    fun `ProductBrand list asResponse maps each`() {
        assertEquals(2, listOf(ProductBrand(), ProductBrand()).groupAsResponse().size)
    }

    @Test
    fun `ProductCategory asResponse maps fields`() {
        val response = ProductCategory().apply { uid = "C1"; name = "Cat"; refId = "RC" }.groupAsResponse()
        assertEquals("C1", response.id)
        assertEquals("Cat", response.name)
        assertEquals("RC", response.refId)
    }

    @Test
    fun `ProductCategory list asResponse maps each`() {
        assertEquals(1, listOf(ProductCategory().apply { uid = "C1" }).groupAsResponse().size)
    }

    @Test
    fun `ProductSubCategory asResponse maps fields`() {
        val response = ProductSubCategory().apply { uid = "S1"; name = "Sub"; refId = "RS" }.groupAsResponse()
        assertEquals("S1", response.id)
        assertEquals("Sub", response.name)
        assertEquals("RS", response.refId)
    }

    @Test
    fun `ProductSubCategory list asResponse maps each`() {
        assertEquals(1, listOf(ProductSubCategory().apply { uid = "S1" }).groupAsResponse().size)
    }

    @Test
    fun `seq id prefixes are non-blank`() {
        assertNotNull(Product().obtainSeqIdPrefix())
        assertTrue(Product().obtainSeqIdPrefix().isNotBlank())
        assertTrue(ProductGroup().obtainSeqIdPrefix().isNotBlank())
        assertTrue(ProductBrand().obtainSeqIdPrefix().isNotBlank())
        assertTrue(ProductCategory().obtainSeqIdPrefix().isNotBlank())
        assertTrue(ProductSubCategory().obtainSeqIdPrefix().isNotBlank())
    }
}
