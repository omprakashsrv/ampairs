package com.ampairs.product.service

import com.ampairs.event.domain.events.ProductCatalogChangedEvent
import com.ampairs.event.domain.events.ProductUpdatedEvent
import com.ampairs.file.domain.dto.EntityImageListResponse
import com.ampairs.file.domain.dto.EntityImageResponse
import com.ampairs.product.domain.model.Product
import com.ampairs.product.domain.model.group.ProductBrand
import com.ampairs.product.domain.model.group.ProductCategory
import com.ampairs.product.domain.model.group.ProductGroup
import com.ampairs.product.domain.model.group.ProductSubCategory
import com.ampairs.product.repository.ProductBrandRepository
import com.ampairs.product.repository.ProductCategoryRepository
import com.ampairs.product.repository.ProductGroupRepository
import com.ampairs.product.repository.ProductPagingRepository
import com.ampairs.product.repository.ProductRepository
import com.ampairs.product.repository.ProductSubCategoryRepository
import com.ampairs.unit.service.UnitService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.Instant
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceTest {

    @Mock private lateinit var productPagingRepository: ProductPagingRepository
    @Mock private lateinit var unitService: UnitService
    @Mock private lateinit var productGroupRepository: ProductGroupRepository
    @Mock private lateinit var productBrandRepository: ProductBrandRepository
    @Mock private lateinit var productCategoryRepository: ProductCategoryRepository
    @Mock private lateinit var productSubCategoryRepository: ProductSubCategoryRepository
    @Mock private lateinit var productRepository: ProductRepository
    @Mock private lateinit var eventPublisher: ApplicationEventPublisher
    @Mock private lateinit var entityChangePublisher: com.ampairs.core.sync.EntityChangePublisher
    @Mock private lateinit var entityImageService: com.ampairs.file.domain.service.EntityImageService

    private lateinit var service: ProductService

    private fun product(block: Product.() -> Unit = {}): Product = Product().apply {
        uid = "PRD-1"
        name = "Widget"
        code = "W1"
        sku = "SKU-1"
        status = "ACTIVE"
        basePrice = 10.0
        sellingPrice = 12.0
        mrp = 15.0
        block()
    }

    @BeforeEach
    fun setUp() {
        service = ProductService(
            productPagingRepository = productPagingRepository,
            unitService = unitService,
            productGroupRepository = productGroupRepository,
            productBrandRepository = productBrandRepository,
            productCategoryRepository = productCategoryRepository,
            productSubCategoryRepository = productSubCategoryRepository,
            productRepository = productRepository,
            eventPublisher = eventPublisher,
            entityChangePublisher = entityChangePublisher,
            entityImageService = entityImageService,
        )
        whenever(productRepository.save(any<Product>())).thenAnswer { it.arguments[0] }
        whenever(productGroupRepository.save(any<ProductGroup>())).thenAnswer { it.arguments[0] }
        whenever(productBrandRepository.save(any<ProductBrand>())).thenAnswer { it.arguments[0] }
        whenever(productCategoryRepository.save(any<ProductCategory>())).thenAnswer { it.arguments[0] }
        whenever(productSubCategoryRepository.save(any<ProductSubCategory>())).thenAnswer { it.arguments[0] }
    }

    // ---------- getProducts(updatedAt) ----------

    @Test
    fun `getProducts defaults to EPOCH when updatedAt is null`() {
        whenever(
            productPagingRepository.findAllByUpdatedAtGreaterThanEqual(any(), any())
        ).thenReturn(listOf(product()))

        val result = service.getProducts(updatedAt = null)

        assertEquals(1, result.size)
        assertEquals("PRD-1", result.first().id)
        verify(productPagingRepository).findAllByUpdatedAtGreaterThanEqual(eq(Instant.EPOCH), any())
    }

    @Test
    fun `getProducts passes through provided updatedAt`() {
        val ts = Instant.parse("2025-01-01T00:00:00Z")
        whenever(
            productPagingRepository.findAllByUpdatedAtGreaterThanEqual(eq(ts), any())
        ).thenReturn(emptyList())

        val result = service.getProducts(updatedAt = ts)

        assertTrue(result.isEmpty())
        verify(productPagingRepository).findAllByUpdatedAtGreaterThanEqual(eq(ts), any())
    }

    // ---------- getProductsAfterSync ----------

    @Test
    fun `getProductsAfterSync with blank lastSync returns all rows`() {
        whenever(productPagingRepository.findAllBy(any()))
            .thenReturn(PageImpl(listOf(product())))

        val page = service.getProductsAfterSync(lastSync = "  ", pageable = PageRequest.of(0, 10))

        assertEquals(1, page.totalElements)
        assertEquals("PRD-1", page.content.first().id)
        verify(productPagingRepository).findAllBy(any())
    }

    @Test
    fun `getProductsAfterSync with valid lastSync uses incremental query`() {
        whenever(productPagingRepository.findByUpdatedAtGreaterThanEqual(any(), any()))
            .thenReturn(PageImpl(listOf(product())))

        val page = service.getProductsAfterSync(lastSync = "2025-01-01T00:00:00Z", pageable = PageRequest.of(0, 10))

        assertEquals(1, page.totalElements)
        verify(productPagingRepository).findByUpdatedAtGreaterThanEqual(any(), any())
        verify(productPagingRepository, never()).findAllBy(any())
    }

    @Test
    fun `getProductsAfterSync with unparseable lastSync falls back to all rows`() {
        whenever(productPagingRepository.findAllBy(any()))
            .thenReturn(PageImpl(emptyList<Product>()))

        val page = service.getProductsAfterSync(lastSync = "not-a-date", pageable = PageRequest.of(0, 10))

        assertEquals(0, page.totalElements)
        verify(productPagingRepository).findAllBy(any())
    }

    // ---------- updateProducts ----------

    @Test
    fun `updateProducts inserts new products and publishes UPDATED change`() {
        val incoming = product { uid = "" }
        whenever(productRepository.saveAll(any<List<Product>>())).thenAnswer { it.arguments[0] }

        val result = service.updateProducts(listOf(incoming))

        assertEquals(1, result.size)
        verify(productRepository).saveAll(any<List<Product>>())
        verify(entityChangePublisher).publish(eq("product"), any(), eq(com.ampairs.core.sync.EntityChangeType.UPDATED))
    }

    @Test
    fun `updateProducts merges scalar fields onto existing by uid`() {
        val existing = product { name = "Old"; basePrice = 1.0 }
        val incoming = product { name = "New"; basePrice = 99.0; refId = "TALLY-1" }
        whenever(productRepository.findAllByUidIn(listOf("PRD-1"))).thenReturn(listOf(existing))
        whenever(productRepository.saveAll(any<List<Product>>())).thenAnswer { it.arguments[0] }

        val result = service.updateProducts(listOf(incoming))

        assertEquals("New", existing.name)
        assertEquals(99.0, existing.basePrice)
        assertEquals("TALLY-1", existing.refId)
        assertEquals("New", result.first().name)
    }

    @Test
    fun `updateProducts resolves existing by refId when uid is blank`() {
        val existing = product { uid = "PRD-EXIST"; refId = "REF-9"; name = "Old" }
        val incoming = product { uid = ""; refId = "REF-9"; name = "Fresh" }
        whenever(productRepository.findAllByRefIdIn(listOf("REF-9"))).thenReturn(listOf(existing))
        whenever(productRepository.saveAll(any<List<Product>>())).thenAnswer { it.arguments[0] }

        service.updateProducts(listOf(incoming))

        assertEquals("Fresh", existing.name)
    }

    @Test
    fun `updateProducts validates unit and throws when missing`() {
        val incoming = product { uid = ""; unitId = "UNIT-X" }
        whenever(unitService.findByUid("UNIT-X")).thenReturn(null)

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            service.updateProducts(listOf(incoming))
        }
    }

    @Test
    fun `updateProducts publishes DELETED change for deleted products`() {
        val incoming = product { uid = "PRD-DEL"; status = "DELETED" }
        whenever(productRepository.findAllByUidIn(any())).thenReturn(emptyList())
        whenever(productRepository.saveAll(any<List<Product>>())).thenAnswer { it.arguments[0] }

        service.updateProducts(listOf(incoming))

        verify(entityChangePublisher).publish(eq("product"), eq("PRD-DEL"), eq(com.ampairs.core.sync.EntityChangeType.DELETED))
    }

    @Test
    fun `updateProducts emits catalog event with unlist for ecom-listed deleted product`() {
        val incoming = product { uid = "PRD-LISTED"; status = "DELETED"; isEcomListed = true }
        whenever(productRepository.findAllByUidIn(any())).thenReturn(emptyList())
        whenever(productRepository.saveAll(any<List<Product>>())).thenAnswer { it.arguments[0] }

        service.updateProducts(listOf(incoming))

        val captor = ArgumentCaptor.forClass(ProductCatalogChangedEvent::class.java)
        verify(eventPublisher).publishEvent(captor.capture())
        val change = captor.value.changes.single()
        assertEquals("PRD-LISTED", change.managementProductId)
        assertFalse(change.listed)
    }

    @Test
    fun `updateProducts emits catalog refresh for ecom-listed active product`() {
        val incoming = product { uid = "PRD-ACTIVE"; status = "ACTIVE"; isEcomListed = true; sellingPrice = 50.0; mrp = 60.0 }
        whenever(productRepository.findAllByUidIn(any())).thenReturn(emptyList())
        whenever(productRepository.saveAll(any<List<Product>>())).thenAnswer { it.arguments[0] }
        whenever(entityImageService.getImages(any(), any(), any())).thenReturn(emptyImageList())

        service.updateProducts(listOf(incoming))

        val captor = ArgumentCaptor.forClass(ProductCatalogChangedEvent::class.java)
        verify(eventPublisher).publishEvent(captor.capture())
        val change = captor.value.changes.single()
        assertTrue(change.listed)
        assertEquals(50.0, change.price?.toDouble())
        assertEquals(60.0, change.mrp?.toDouble())
        assertEquals(100, change.stockQuantity)
    }

    @Test
    fun `updateProducts does not emit catalog event for never-listed products`() {
        val incoming = product { uid = "PRD-NORMAL"; isEcomListed = false }
        whenever(productRepository.findAllByUidIn(any())).thenReturn(emptyList())
        whenever(productRepository.saveAll(any<List<Product>>())).thenAnswer { it.arguments[0] }

        service.updateProducts(listOf(incoming))

        verify(eventPublisher, never()).publishEvent(any())
    }

    @Test
    fun `updateProducts with empty list does nothing`() {
        whenever(productRepository.saveAll(any<List<Product>>())).thenReturn(emptyList())

        val result = service.updateProducts(emptyList())

        assertTrue(result.isEmpty())
        verify(eventPublisher, never()).publishEvent(any())
    }

    // ---------- setEcomListing ----------

    @Test
    fun `setEcomListing returns null when product not found`() {
        whenever(productRepository.findByUid("MISSING")).thenReturn(null)

        assertNull(service.setEcomListing("MISSING", true))
        verify(eventPublisher, never()).publishEvent(any())
    }

    @Test
    fun `setEcomListing true publishes listed catalog change`() {
        val p = product { sellingPrice = 0.0; mrp = 20.0 }
        whenever(productRepository.findByUid("PRD-1")).thenReturn(p)
        whenever(entityImageService.getImages(any(), any(), any())).thenReturn(emptyImageList())

        val response = service.setEcomListing("PRD-1", true)

        assertNotNull(response)
        assertTrue(p.isEcomListed)
        val captor = ArgumentCaptor.forClass(ProductCatalogChangedEvent::class.java)
        verify(eventPublisher).publishEvent(captor.capture())
        val change = captor.value.changes.single()
        assertTrue(change.listed)
        // sellingPrice is 0, so price falls back to mrp (20.0)
        assertEquals(20.0, change.price?.toDouble())
    }

    @Test
    fun `setEcomListing false publishes unlist catalog change`() {
        val p = product { isEcomListed = true }
        whenever(productRepository.findByUid("PRD-1")).thenReturn(p)

        service.setEcomListing("PRD-1", false)

        assertFalse(p.isEcomListed)
        val captor = ArgumentCaptor.forClass(ProductCatalogChangedEvent::class.java)
        verify(eventPublisher).publishEvent(captor.capture())
        assertFalse(captor.value.changes.single().listed)
    }

    @Test
    fun `setEcomListing resolves image urls primary first`() {
        val p = product { isEcomListed = false }
        whenever(productRepository.findByUid("PRD-1")).thenReturn(p)
        whenever(entityImageService.getImages(eq("PRODUCT"), eq("PRD-1"), any())).thenReturn(
            EntityImageListResponse(
                images = listOf(
                    imageResponse(uid = "IMG-2", url = "https://b.jpg", primary = false),
                    imageResponse(uid = "IMG-1", url = "https://a.jpg", primary = true),
                ),
                totalCount = 2, primaryImage = null, totalSize = 0L, formattedTotalSize = "0 B",
            )
        )

        service.setEcomListing("PRD-1", true)

        val captor = ArgumentCaptor.forClass(ProductCatalogChangedEvent::class.java)
        verify(eventPublisher).publishEvent(captor.capture())
        val urls = captor.value.changes.single().imageUrls
        assertEquals(listOf("https://a.jpg", "https://b.jpg"), urls)
    }

    @Test
    fun `setEcomListing tolerates image service failure`() {
        val p = product { isEcomListed = false }
        whenever(productRepository.findByUid("PRD-1")).thenReturn(p)
        whenever(entityImageService.getImages(any(), any(), any())).thenThrow(RuntimeException("boom"))

        val response = service.setEcomListing("PRD-1", true)

        assertNotNull(response)
        val captor = ArgumentCaptor.forClass(ProductCatalogChangedEvent::class.java)
        verify(eventPublisher).publishEvent(captor.capture())
        assertTrue(captor.value.changes.single().imageUrls!!.isEmpty())
    }

    // ---------- updateProductGroups / brands / categories / subcategories ----------

    @Test
    fun `updateProductGroups assigns id and refId from existing by uid`() {
        val existing = ProductGroup().apply { id = 7; uid = "GRP-1"; refId = "OLD" }
        whenever(productGroupRepository.findByUid("GRP-1")).thenReturn(existing)
        val incoming = ProductGroup().apply { uid = "GRP-1"; name = "Tools" }

        val result = service.updateProductGroups(listOf(incoming))

        assertEquals(7, incoming.id)
        assertEquals("OLD", incoming.refId)
        assertEquals(1, result.size)
        verify(entityChangePublisher).updated(eq("product_catalog"), eq("GRP-1"))
    }

    @Test
    fun `updateProductGroups resolves by refId when uid blank`() {
        val existing = ProductGroup().apply { id = 3; uid = "GRP-9"; refId = "REF-1" }
        whenever(productGroupRepository.findByRefId("REF-1")).thenReturn(existing)
        val incoming = ProductGroup().apply { uid = ""; refId = "REF-1"; name = "X" }

        service.updateProductGroups(listOf(incoming))

        assertEquals(3, incoming.id)
        assertEquals("GRP-9", incoming.uid)
    }

    @Test
    fun `updateProductGroups new group keeps id 0`() {
        whenever(productGroupRepository.findByUid("GRP-NEW")).thenReturn(null)
        val incoming = ProductGroup().apply { uid = "GRP-NEW"; name = "Fresh" }

        service.updateProductGroups(listOf(incoming))

        assertEquals(0, incoming.id)
    }

    @Test
    fun `updateProductGroups empty list publishes nothing`() {
        service.updateProductGroups(emptyList())
        verify(entityChangePublisher, never()).updated(any(), any())
    }

    @Test
    fun `updateProductBrands assigns id from existing`() {
        val existing = ProductBrand().apply { id = 5; uid = "BRD-1"; refId = "B" }
        whenever(productBrandRepository.findByUid("BRD-1")).thenReturn(existing)
        val incoming = ProductBrand().apply { uid = "BRD-1"; name = "Acme" }

        service.updateProductBrands(listOf(incoming))

        assertEquals(5, incoming.id)
        assertEquals("B", incoming.refId)
        verify(entityChangePublisher).updated(eq("product_catalog"), eq("BRD-1"))
    }

    @Test
    fun `updateProductBrands resolves by refId`() {
        val existing = ProductBrand().apply { id = 2; uid = "BRD-2"; refId = "RB" }
        whenever(productBrandRepository.findByRefId("RB")).thenReturn(existing)
        val incoming = ProductBrand().apply { uid = ""; refId = "RB" }

        service.updateProductBrands(listOf(incoming))

        assertEquals(2, incoming.id)
        assertEquals("BRD-2", incoming.uid)
    }

    @Test
    fun `updateProductCategories assigns id from existing`() {
        val existing = ProductCategory().apply { id = 11; uid = "CAT-1"; refId = "C" }
        whenever(productCategoryRepository.findByUid("CAT-1")).thenReturn(existing)
        val incoming = ProductCategory().apply { uid = "CAT-1"; name = "Hardware" }

        service.updateProductCategories(listOf(incoming))

        assertEquals(11, incoming.id)
        verify(entityChangePublisher).updated(eq("product_catalog"), eq("CAT-1"))
    }

    @Test
    fun `updateProductCategories resolves by refId`() {
        val existing = ProductCategory().apply { id = 4; uid = "CAT-2"; refId = "RC" }
        whenever(productCategoryRepository.findByRefId("RC")).thenReturn(existing)
        val incoming = ProductCategory().apply { uid = ""; refId = "RC" }

        service.updateProductCategories(listOf(incoming))

        assertEquals("CAT-2", incoming.uid)
    }

    @Test
    fun `updateProductSubCategories assigns id from existing`() {
        val existing = ProductSubCategory().apply { id = 8; uid = "SUB-1"; refId = "S" }
        whenever(productSubCategoryRepository.findByUid("SUB-1")).thenReturn(existing)
        val incoming = ProductSubCategory().apply { uid = "SUB-1"; name = "Bolts" }

        service.updateProductSubCategories(listOf(incoming))

        assertEquals(8, incoming.id)
        verify(entityChangePublisher).updated(eq("product_catalog"), eq("SUB-1"))
    }

    @Test
    fun `updateProductSubCategories resolves by refId`() {
        val existing = ProductSubCategory().apply { id = 6; uid = "SUB-2"; refId = "RS" }
        whenever(productSubCategoryRepository.findByRefId("RS")).thenReturn(existing)
        val incoming = ProductSubCategory().apply { uid = ""; refId = "RS" }

        service.updateProductSubCategories(listOf(incoming))

        assertEquals("SUB-2", incoming.uid)
    }

    // ---------- *AfterSync feeds ----------

    @Test
    fun `getGroupsAfterSync blank returns all paged`() {
        whenever(productGroupRepository.findAllPaged(any())).thenReturn(PageImpl(listOf(ProductGroup())))
        val page = service.getGroupsAfterSync(null, PageRequest.of(0, 10))
        assertEquals(1, page.totalElements)
        verify(productGroupRepository).findAllPaged(any())
    }

    @Test
    fun `getGroupsAfterSync valid timestamp uses after query`() {
        whenever(productGroupRepository.findByUpdatedAtAfter(any(), any())).thenReturn(PageImpl(emptyList<ProductGroup>()))
        service.getGroupsAfterSync("2025-01-01T00:00:00Z", PageRequest.of(0, 10))
        verify(productGroupRepository).findByUpdatedAtAfter(any(), any())
    }

    @Test
    fun `getBrandsAfterSync invalid timestamp falls back to all`() {
        whenever(productBrandRepository.findAllPaged(any())).thenReturn(PageImpl(emptyList<ProductBrand>()))
        service.getBrandsAfterSync("bad", PageRequest.of(0, 10))
        verify(productBrandRepository).findAllPaged(any())
    }

    @Test
    fun `getCategoriesAfterSync valid uses after query`() {
        whenever(productCategoryRepository.findByUpdatedAtAfter(any(), any())).thenReturn(PageImpl(emptyList<ProductCategory>()))
        service.getCategoriesAfterSync("2025-02-02T10:00:00Z", PageRequest.of(0, 10))
        verify(productCategoryRepository).findByUpdatedAtAfter(any(), any())
    }

    @Test
    fun `getSubCategoriesAfterSync blank returns all`() {
        whenever(productSubCategoryRepository.findAllPaged(any())).thenReturn(PageImpl(emptyList<ProductSubCategory>()))
        service.getSubCategoriesAfterSync("", PageRequest.of(0, 10))
        verify(productSubCategoryRepository).findAllPaged(any())
    }

    // ---------- simple getters ----------

    @Test
    fun `getGroups returns repository list`() {
        whenever(productGroupRepository.findAll()).thenReturn(listOf(ProductGroup(), ProductGroup()))
        assertEquals(2, service.getGroups().size)
    }

    @Test
    fun `getBrands returns repository list`() {
        whenever(productBrandRepository.findAll()).thenReturn(listOf(ProductBrand()))
        assertEquals(1, service.getBrands().size)
    }

    @Test
    fun `getSubCategories returns repository list`() {
        whenever(productSubCategoryRepository.findAll()).thenReturn(listOf(ProductSubCategory()))
        assertEquals(1, service.getSubCategories().size)
    }

    @Test
    fun `getCategories no-arg returns all`() {
        whenever(productCategoryRepository.findAll()).thenReturn(listOf(ProductCategory()))
        assertEquals(1, service.getCategories().size)
    }

    @Test
    fun `getCategories by ids delegates to findBySeqIds`() {
        whenever(productCategoryRepository.findBySeqIds(any())).thenReturn(listOf(ProductCategory()))
        val result = service.getCategories(setOf("CAT-1", "CAT-2"))
        assertEquals(1, result.size)
        verify(productCategoryRepository).findBySeqIds(any())
    }

    @Test
    fun `getProducts by groupId maps to responses`() {
        whenever(productRepository.getProduct("GRP-1")).thenReturn(listOf(product()))
        val result = service.getProducts("GRP-1")
        assertEquals("PRD-1", result.single().id)
    }

    // ---------- createProduct ----------

    @Test
    fun `createProduct generates SKU when blank and sets status ACTIVE`() {
        val p = product { uid = "PRD-NEW"; sku = null; status = "DRAFT"; name = "Hammer"; categoryId = "tools" }

        val saved = service.createProduct(p)

        assertEquals("ACTIVE", saved.status)
        assertNotNull(saved.sku)
        assertTrue(saved.sku!!.startsWith("TOO-HAM-"))
        verify(eventPublisher).publishEvent(any())
    }

    @Test
    fun `createProduct keeps provided SKU`() {
        val p = product { uid = "PRD-NEW"; sku = "MY-SKU" }
        val saved = service.createProduct(p)
        assertEquals("MY-SKU", saved.sku)
    }

    @Test
    fun `createProduct uses PRD prefix when categoryId null`() {
        val p = product { uid = "PRD-NEW"; sku = ""; categoryId = null; name = "ab" }
        val saved = service.createProduct(p)
        assertTrue(saved.sku!!.startsWith("PRD-AB-"))
    }

    @Test
    fun `createProduct validates unit and throws when missing`() {
        val p = product { unitId = "UNIT-MISSING" }
        whenever(unitService.findByUid("UNIT-MISSING")).thenReturn(null)
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> { service.createProduct(p) }
    }

    // ---------- updateProduct ----------

    @Test
    fun `updateProduct returns null when product not found`() {
        whenever(productRepository.findByUid("MISSING")).thenReturn(null)
        assertNull(service.updateProduct("MISSING", Product()))
    }

    @Test
    fun `updateProduct applies changed fields and publishes update event`() {
        val existing = product { name = "Old"; description = "d"; basePrice = 1.0; costPrice = 2.0; status = "ACTIVE" }
        whenever(productRepository.findByUid("PRD-1")).thenReturn(existing)
        val updates = Product().apply {
            name = "New Name"
            description = "new desc"
            basePrice = 100.0
            costPrice = 50.0
            status = "INACTIVE"
            attributes = mapOf("color" to "red")
        }

        val response = service.updateProduct("PRD-1", updates)

        assertNotNull(response)
        assertEquals("New Name", existing.name)
        assertEquals("new desc", existing.description)
        assertEquals(100.0, existing.basePrice)
        assertEquals(50.0, existing.costPrice)
        assertEquals("INACTIVE", existing.status)
        assertEquals(mapOf("color" to "red"), existing.attributes)
        verify(eventPublisher).publishEvent(any<ProductUpdatedEvent>())
    }

    @Test
    fun `updateProduct does not publish event when nothing changed`() {
        val existing = product { name = "Same" }
        whenever(productRepository.findByUid("PRD-1")).thenReturn(existing)
        // updates with blank/zero fields -> no changes detected
        val updates = Product().apply { name = ""; basePrice = 0.0; costPrice = 0.0; status = "" }

        val response = service.updateProduct("PRD-1", updates)

        assertNotNull(response)
        verify(eventPublisher, never()).publishEvent(any())
    }

    // ---------- searchProducts ----------

    @Test
    fun `searchProducts by search term`() {
        whenever(productRepository.searchProducts(eq("widget"), any())).thenReturn(PageImpl(listOf(product())))
        val page = service.searchProducts("widget", null, null, null, null, PageRequest.of(0, 10))
        assertEquals(1, page.totalElements)
        verify(productRepository).searchProducts(eq("widget"), any())
    }

    @Test
    fun `searchProducts by category`() {
        whenever(productRepository.findActiveProductsByCategory(eq("CAT"), any())).thenReturn(PageImpl(emptyList<Product>()))
        service.searchProducts(null, "CAT", null, null, null, PageRequest.of(0, 10))
        verify(productRepository).findActiveProductsByCategory(eq("CAT"), any())
    }

    @Test
    fun `searchProducts by brand`() {
        whenever(productRepository.findActiveProductsByBrand(eq("BRD"), any())).thenReturn(PageImpl(emptyList<Product>()))
        service.searchProducts(null, null, "BRD", null, null, PageRequest.of(0, 10))
        verify(productRepository).findActiveProductsByBrand(eq("BRD"), any())
    }

    @Test
    fun `searchProducts by price range`() {
        whenever(productRepository.findActiveProductsByPriceRange(eq(1.0), eq(9.0), any())).thenReturn(PageImpl(emptyList<Product>()))
        service.searchProducts(null, null, null, 1.0, 9.0, PageRequest.of(0, 10))
        verify(productRepository).findActiveProductsByPriceRange(eq(1.0), eq(9.0), any())
    }

    @Test
    fun `searchProducts default returns active products`() {
        whenever(productRepository.findByStatus("ACTIVE")).thenReturn(listOf(product(), product()))
        val page = service.searchProducts(null, null, null, null, null, PageRequest.of(0, 10))
        assertEquals(2, page.totalElements)
        verify(productRepository).findByStatus("ACTIVE")
    }

    // ---------- single getters ----------

    @Test
    fun `getProductByUid returns mapped response`() {
        whenever(productRepository.findByUid("PRD-1")).thenReturn(product())
        assertEquals("PRD-1", service.getProductByUid("PRD-1")?.id)
    }

    @Test
    fun `getProductByUid returns null when missing`() {
        whenever(productRepository.findByUid("X")).thenReturn(null)
        assertNull(service.getProductByUid("X"))
    }

    @Test
    fun `getProductBySku returns mapped response`() {
        whenever(productRepository.findBySku("SKU-1")).thenReturn(Optional.of(product()))
        assertEquals("PRD-1", service.getProductBySku("SKU-1")?.id)
    }

    @Test
    fun `getProductBySku returns null when empty`() {
        whenever(productRepository.findBySku("NONE")).thenReturn(Optional.empty())
        assertNull(service.getProductBySku("NONE"))
    }

    @Test
    fun `getActiveProducts returns active list`() {
        whenever(productRepository.findByStatus("ACTIVE")).thenReturn(listOf(product()))
        assertEquals(1, service.getActiveProducts(Pageable.unpaged()).size)
    }

    // ---------- helpers ----------

    private fun emptyImageList() = EntityImageListResponse(
        images = emptyList(), totalCount = 0, primaryImage = null, totalSize = 0L, formattedTotalSize = "0 B",
    )

    private fun imageResponse(uid: String, url: String, primary: Boolean) = EntityImageResponse(
        uid = uid, entityType = "PRODUCT", entityUid = "PRD-1", originalFilename = "f.jpg",
        fileExtension = "jpg", contentType = "image/jpeg", fileSize = 1L, formattedFileSize = "1 B",
        storagePath = "p", storageUrl = null, imageUrl = url, thumbnailUrl = url, isPrimary = primary,
        displayOrder = 0, description = null, width = null, height = null, checksum = "c",
        uploadedAt = Instant.EPOCH, active = true, etag = null, lastModified = null,
        createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
    )
}
