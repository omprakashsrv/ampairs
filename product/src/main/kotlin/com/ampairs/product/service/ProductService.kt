package com.ampairs.product.service

import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.event.domain.events.ProductCreatedEvent
import com.ampairs.event.domain.events.ProductUpdatedEvent
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
import java.time.Instant

@Service
class ProductService(
    val productPagingRepository: ProductPagingRepository,
    private val unitService: UnitService,
    val productGroupRepository: ProductGroupRepository,
    val productBrandRepository: ProductBrandRepository,
    val productCategoryRepository: ProductCategoryRepository,
    val productSubCategoryRepository: ProductSubCategoryRepository,
    val productRepository: ProductRepository,
    val eventPublisher: ApplicationEventPublisher
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


    fun getProducts(updatedAt: Instant?): List<Product> {
        return productPagingRepository.findAllByUpdatedAtGreaterThanEqual(
            updatedAt ?: Instant.MIN,
            PageRequest.of(0, 1000, Sort.by("lastUpdated").ascending())
        )
    }

    @Transactional
    fun updateProducts(products: List<Product>): List<Product> {
        products.forEach {
            it.unitId?.takeIf { unitId -> unitId.isNotBlank() }?.let { unitId ->
                unitService.findByUid(unitId)
                    ?: throw IllegalArgumentException("Unit not found for id: $unitId")
            }
            if (it.uid.isNotEmpty()) {
                val group = productRepository.findByUid(it.uid)
                it.id = group?.id ?: 0
                it.refId = group?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val group = productRepository.findByRefId(it.refId)
                it.id = group?.id ?: 0
                it.uid = group?.uid ?: ""
            }
            productRepository.save(it)
        }
        return products
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
        return productSubCategories
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

    fun getProducts(groupId: String): List<Product> {
        return productRepository.getProduct(groupId)
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
        if (product.sku.isBlank()) {
            product.sku = generateSku(product.name, product.categoryId)
        }

        // Validate SKU uniqueness
        if (productRepository.findBySku(product.sku).isPresent) {
            throw IllegalArgumentException("SKU already exists: ${product.sku}")
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
        if (updates.attributes?.isNotEmpty() == true && updates.attributes != existingProduct.attributes) {
            fieldChanges["attributes"] = mapOf("old" to existingProduct.attributes, "new" to updates.attributes!!)
            existingProduct.attributes = updates.attributes
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
