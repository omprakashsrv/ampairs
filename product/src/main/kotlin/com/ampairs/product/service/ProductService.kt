package com.ampairs.product.service

import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.event.domain.events.ProductCreatedEvent
import com.ampairs.event.domain.events.ProductUpdatedEvent
import com.ampairs.product.domain.dto.product.ProductResponse
import com.ampairs.product.domain.dto.product.asResponse
import com.ampairs.product.domain.model.Product
import com.ampairs.product.domain.model.group.ProductBrand
import com.ampairs.product.domain.model.group.ProductCategory
import com.ampairs.product.domain.model.group.ProductGroup
import com.ampairs.product.domain.model.group.ProductSubCategory
import com.ampairs.product.repository.*
import com.ampairs.unit.service.UnitService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

@Service
@Transactional(readOnly = true)
class ProductService(
    val productPagingRepository: ProductPagingRepository,
    private val unitService: UnitService,
    val productGroupRepository: ProductGroupRepository,
    val productBrandRepository: ProductBrandRepository,
    val productCategoryRepository: ProductCategoryRepository,
    val productSubCategoryRepository: ProductSubCategoryRepository,
    val productRepository: ProductRepository,
    val eventPublisher: ApplicationEventPublisher,
    private val entityChangePublisher: EntityChangePublisher,
) {

    /**
     * Helper methods for event publishing
     */
    private fun getWorkspaceId(): String = TenantContextHolder.getCurrentTenant() ?: ""

    private fun getUserId(): String {
        val auth = SecurityContextHolder.getContext().authentication
        return auth?.let { AuthenticationHelper.getCurrentUserId(it) } ?: ""
    }

    private fun getDeviceId(): String = DeviceContextHolder.getCurrentDevice() ?: ""


    fun getProducts(updatedAt: Instant?): List<ProductResponse> {
        return productPagingRepository.findAllByUpdatedAtGreaterThanEqual(
            updatedAt ?: Instant.EPOCH,
            PageRequest.of(0, 1000, Sort.by("updatedAt").ascending())
        ).asResponse()
    }

    /**
     * Incremental sync feed for products — returns rows with updatedAt >= lastSync,
     * INCLUDING soft-deleted (status = "DELETED") rows so clients can detect deletions.
     * Blank/null lastSync returns all rows (paginated) including soft-deleted.
     * Note: @TenantId automatically filters by current workspace.
     */
    fun getProductsAfterSync(lastSync: String?, pageable: Pageable): Page<Product> {
        return if (lastSync.isNullOrBlank()) {
            productPagingRepository.findAllBy(pageable)
        } else {
            try {
                val decodedLastSync = URLDecoder.decode(lastSync, StandardCharsets.UTF_8)
                val lastSyncInstant = Instant.parse(decodedLastSync)
                productPagingRepository.findByUpdatedAtGreaterThanEqual(lastSyncInstant, pageable)
            } catch (e: Exception) {
                productPagingRepository.findAllBy(pageable)
            }
        }
    }

    @Transactional
    fun updateProducts(products: List<Product>): List<ProductResponse> {
        val uids = products.filter { it.uid.isNotEmpty() }.map { it.uid }
        val byUid = if (uids.isNotEmpty())
            productRepository.findAllByUidIn(uids).associateBy { it.uid }
        else emptyMap()

        val refIds = products.filter { it.refId?.isNotEmpty() == true }.mapNotNull { it.refId }
        val byRefId = if (refIds.isNotEmpty())
            productRepository.findAllByRefIdIn(refIds).associateBy { it.refId }
        else emptyMap()

        val entitiesToSave = products.map { incoming ->
            incoming.unitId?.takeIf { it.isNotBlank() }?.let { unitId ->
                unitService.findByUid(unitId)
                    ?: throw IllegalArgumentException("Unit not found for id: $unitId")
            }

            val existing = when {
                incoming.uid.isNotEmpty() -> byUid[incoming.uid]
                incoming.refId?.isNotEmpty() == true -> byRefId[incoming.refId]
                else -> null
            }

            if (existing != null) {
                // Update scalar fields on the managed entity so Hibernate never sees the
                // immutable @OneToOne associations as "changed" (avoids HHH000502).
                existing.name = incoming.name
                existing.code = incoming.code
                existing.sku = incoming.sku
                existing.description = incoming.description
                existing.status = incoming.status
                existing.taxCode = incoming.taxCode
                existing.taxCodeId = incoming.taxCodeId
                existing.unitId = incoming.unitId
                existing.basePrice = incoming.basePrice
                existing.costPrice = incoming.costPrice
                existing.groupId = incoming.groupId
                existing.brandId = incoming.brandId
                existing.categoryId = incoming.categoryId
                existing.subCategoryId = incoming.subCategoryId
                existing.baseUnitId = incoming.baseUnitId
                existing.attributes = incoming.attributes
                existing.mrp = incoming.mrp
                existing.dp = incoming.dp
                existing.sellingPrice = incoming.sellingPrice
                existing
            } else {
                incoming
            }
        }

        return productRepository.saveAll(entitiesToSave).toList().asResponse()
    }


    @Transactional
    fun updateProductGroups(groups: List<ProductGroup>): List<ProductGroup> {
        groups.forEach {
            if (it.uid.isNotEmpty()) {
                val group = productGroupRepository.findByUid(it.uid)
                it.id = group?.id ?: 0
                it.refId = group?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val group = productGroupRepository.findByRefId(it.refId)
                it.id = group?.id ?: 0
                it.uid = group?.uid ?: ""
            }
            productGroupRepository.save(it)
        }
        if (groups.isNotEmpty()) entityChangePublisher.updated("product_catalog", groups.first().uid)
        return groups
    }

    @Transactional
    fun updateProductBrands(brands: List<ProductBrand>): List<ProductBrand> {
        brands.forEach {
            if (it.uid.isNotEmpty()) {
                val group = productBrandRepository.findByUid(it.uid)
                it.id = group?.id ?: 0
                it.refId = group?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val group = productBrandRepository.findByRefId(it.refId)
                it.id = group?.id ?: 0
                it.uid = group?.uid ?: ""
            }
            productBrandRepository.save(it)
        }
        if (brands.isNotEmpty()) entityChangePublisher.updated("product_catalog", brands.first().uid)
        return brands
    }


    @Transactional
    fun updateProductCategories(productCategories: List<ProductCategory>): List<ProductCategory> {
        productCategories.forEach {
            if (it.uid.isNotEmpty()) {
                val productCategory = productCategoryRepository.findByUid(it.uid)
                it.id = productCategory?.id ?: 0
                it.refId = productCategory?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val productCategory = productCategoryRepository.findByRefId(it.refId)
                it.id = productCategory?.id ?: 0
                it.uid = productCategory?.uid ?: ""
            }
            productCategoryRepository.save(it)
        }
        if (productCategories.isNotEmpty()) entityChangePublisher.updated("product_catalog", productCategories.first().uid)
        return productCategories
    }


    @Transactional
    fun updateProductSubCategories(productSubCategories: List<ProductSubCategory>): List<ProductSubCategory> {
        productSubCategories.forEach {
            if (it.uid.isNotEmpty()) {
                val productCategory = productSubCategoryRepository.findByUid(it.uid)
                it.id = productCategory?.id ?: 0
                it.refId = productCategory?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val productCategory = productSubCategoryRepository.findByRefId(it.refId)
                it.id = productCategory?.id ?: 0
                it.uid = productCategory?.uid ?: ""
            }
            productSubCategoryRepository.save(it)
        }
        if (productSubCategories.isNotEmpty()) entityChangePublisher.updated("product_catalog", productSubCategories.first().uid)
        return productSubCategories
    }

    /**
     * Incremental sync feed for product groups — rows with updatedAt >= lastSync, paginated.
     * Blank/null lastSync returns all rows (paginated). @TenantId filters by workspace.
     */
    fun getGroupsAfterSync(lastSync: String?, pageable: Pageable): Page<ProductGroup> =
        syncPage(lastSync, pageable,
            allFn = { productGroupRepository.findAllPaged(it) },
            afterFn = { ts, p -> productGroupRepository.findByUpdatedAtAfter(ts, p) })

    /** Incremental sync feed for product brands. */
    fun getBrandsAfterSync(lastSync: String?, pageable: Pageable): Page<ProductBrand> =
        syncPage(lastSync, pageable,
            allFn = { productBrandRepository.findAllPaged(it) },
            afterFn = { ts, p -> productBrandRepository.findByUpdatedAtAfter(ts, p) })

    /** Incremental sync feed for product categories. */
    fun getCategoriesAfterSync(lastSync: String?, pageable: Pageable): Page<ProductCategory> =
        syncPage(lastSync, pageable,
            allFn = { productCategoryRepository.findAllPaged(it) },
            afterFn = { ts, p -> productCategoryRepository.findByUpdatedAtAfter(ts, p) })

    /** Incremental sync feed for product sub-categories. */
    fun getSubCategoriesAfterSync(lastSync: String?, pageable: Pageable): Page<ProductSubCategory> =
        syncPage(lastSync, pageable,
            allFn = { productSubCategoryRepository.findAllPaged(it) },
            afterFn = { ts, p -> productSubCategoryRepository.findByUpdatedAtAfter(ts, p) })

    /**
     * Shared parse-and-dispatch for catalog sync feeds: blank/invalid lastSync → all rows,
     * otherwise rows updated at/after the parsed ISO-8601 instant.
     */
    private fun <T : Any> syncPage(
        lastSync: String?,
        pageable: Pageable,
        allFn: (Pageable) -> Page<T>,
        afterFn: (Instant, Pageable) -> Page<T>,
    ): Page<T> {
        return if (lastSync.isNullOrBlank()) {
            allFn(pageable)
        } else {
            try {
                val decoded = URLDecoder.decode(lastSync, StandardCharsets.UTF_8)
                afterFn(Instant.parse(decoded), pageable)
            } catch (e: Exception) {
                allFn(pageable)
            }
        }
    }

    fun getGroups(): List<ProductGroup> {
        return productGroupRepository.findAll().toList()
    }

    fun getBrands(): List<ProductBrand> {
        return productBrandRepository.findAll().toList()
    }

    fun getSubCategories(): List<ProductSubCategory> {
        return productSubCategoryRepository.findAll().toList()
    }

    fun getProducts(groupId: String): List<ProductResponse> {
        return productRepository.getProduct(groupId).asResponse()
    }

    fun getCategories(ids: Set<String>): List<ProductCategory> {
        return productCategoryRepository.findBySeqIds(ids.toList())
    }

    fun getCategories(): List<ProductCategory> {
        return productCategoryRepository.findAll().toMutableList()
    }

    /**
     * Retail-specific methods for enhanced product management
     */

    @Transactional
    fun createProduct(product: Product): Product {
        product.unitId?.takeIf { it.isNotBlank() }?.let { unitId ->
            unitService.findByUid(unitId)
                ?: throw IllegalArgumentException("Unit not found for id: $unitId")
        }
        // Generate SKU if not provided
        if (product.sku.isNullOrBlank()) {
            product.sku = generateSku(product.name, product.categoryId)
        }

        product.status = "ACTIVE"
        val savedProduct = productRepository.save(product)

        // Publish ProductCreatedEvent
        eventPublisher.publishEvent(
            ProductCreatedEvent(
                source = this,
                workspaceId = getWorkspaceId(),
                entityId = savedProduct.uid,
                userId = getUserId(),
                deviceId = getDeviceId(),
                productName = savedProduct.name,
                sku = savedProduct.sku
            )
        )

        return savedProduct
    }

    @Transactional
    fun updateProduct(productId: String, updates: Product): Product? {
        val existingProduct = productRepository.findByUid(productId) ?: return null

        // Track changes for event
        val fieldChanges = mutableMapOf<String, Any>()

        // Update fields and track changes
        if (updates.name.isNotBlank() && updates.name != existingProduct.name) {
            fieldChanges["name"] = mapOf("old" to existingProduct.name, "new" to updates.name)
            existingProduct.name = updates.name
        }
        if (updates.description != null && updates.description != existingProduct.description) {
            fieldChanges["description"] =
                mapOf("old" to (existingProduct.description ?: ""), "new" to updates.description)
            existingProduct.description = updates.description
        }
        if (updates.basePrice > 0 && updates.basePrice != existingProduct.basePrice) {
            fieldChanges["basePrice"] = mapOf("old" to existingProduct.basePrice, "new" to updates.basePrice)
            existingProduct.basePrice = updates.basePrice
        }
        if (updates.costPrice > 0 && updates.costPrice != existingProduct.costPrice) {
            fieldChanges["costPrice"] = mapOf("old" to existingProduct.costPrice, "new" to updates.costPrice)
            existingProduct.costPrice = updates.costPrice
        }
        if (updates.attributes.isNotEmpty() && updates.attributes != existingProduct.attributes) {
            val newAttributes = updates.attributes
            fieldChanges["attributes"] = mapOf("old" to existingProduct.attributes, "new" to newAttributes)
            existingProduct.attributes = newAttributes
        }
        if (updates.status.isNotBlank() && updates.status != existingProduct.status) {
            fieldChanges["status"] = mapOf("old" to existingProduct.status, "new" to updates.status)
            existingProduct.status = updates.status
        }

        val savedProduct = productRepository.save(existingProduct)

        // Publish ProductUpdatedEvent only if there were changes
        if (fieldChanges.isNotEmpty()) {
            eventPublisher.publishEvent(
                ProductUpdatedEvent(
                    source = this,
                    workspaceId = getWorkspaceId(),
                    entityId = savedProduct.uid,
                    userId = getUserId(),
                    deviceId = getDeviceId(),
                    fieldChanges = fieldChanges
                )
            )
        }

        return savedProduct
    }

    fun searchProducts(
        searchTerm: String?, category: String?, brand: String?,
        minPrice: Double?, maxPrice: Double?, pageable: Pageable
    ): Page<Product> {
        return when {
            !searchTerm.isNullOrBlank() -> productRepository.searchProducts(searchTerm, pageable)
            !category.isNullOrBlank() -> productRepository.findActiveProductsByCategory(category, pageable)
            !brand.isNullOrBlank() -> productRepository.findActiveProductsByBrand(brand, pageable)
            minPrice != null && maxPrice != null ->
                productRepository.findActiveProductsByPriceRange(minPrice, maxPrice, pageable)

            else -> productRepository.findByStatus("ACTIVE").let {
                org.springframework.data.domain.PageImpl(it, pageable, it.size.toLong())
            }
        }
    }

    fun getProductByUid(uid: String): Product? = productRepository.findByUid(uid)

    fun getProductBySku(sku: String): Product? {
        return productRepository.findBySku(sku).orElse(null)
    }

    fun getActiveProducts(pageable: Pageable): List<Product> {
        return productRepository.findByStatus("ACTIVE")
    }

    private fun generateSku(name: String, categoryId: String?): String {
        val prefix = categoryId?.take(3)?.uppercase() ?: "PRD"
        val namePart = name.take(3).uppercase().replace(Regex("[^A-Z0-9]"), "")
        val timestamp = System.currentTimeMillis().toString().takeLast(4)
        return "$prefix-$namePart-$timestamp"
    }
}
