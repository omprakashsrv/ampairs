package com.ampairs.product.data.repository

import com.ampairs.product.data.api.ProductApi
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.product.db.entity.ProductEntity
import com.ampairs.product.domain.Product
import com.ampairs.product.domain.ProductListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Repository for product data following Store5 offline-first pattern
 * Similar to CustomerRepository implementation
 */
@OptIn(ExperimentalTime::class)
class ProductRepository(
    private val productApi: ProductApi,
    private val productDao: ProductDao
) {

    /**
     * Get all products as Flow
     */
    fun observeProducts(): Flow<List<ProductListItem>> {
        return flow {
            val products = productDao.getProducts()
            emit(products.map { it.toProductListItem() })
        }
    }

    /**
     * Search products by name or code
     */
    fun searchProducts(query: String): Flow<List<ProductListItem>> {
        return flow {
            val products = productDao.getProductsByName(query)
            emit(products.map { it.toProductListItem() })
        }
    }

    /**
     * Get products by category
     */
    suspend fun getProductsByCategory(categoryIds: List<String>): List<ProductListItem> {
        val products = productDao.productsByCategoryIds(categoryIds)
        return products.map { it.toProductListItem() }
    }

    /**
     * Get a single product (suspending)
     */
    suspend fun getProduct(productId: String): Product? {
        return productDao.productById(productId)?.let { entity ->
            entity.toDomainProduct()
        }
    }

    /**
     * Create a new product
     */
    suspend fun createProduct(product: Product): Result<Product> {
        return try {
            val productWithId = if (product.id.isBlank()) {
                product.copy(id = generateLocalId())
            } else product

            productDao.insert(productWithId.toEntity())
            Result.success(productWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update an existing product
     */
    suspend fun updateProduct(product: Product): Result<Product> {
        return try {
            productDao.update(product.toEntity())
            Result.success(product)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a product
     */
    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            productDao.deleteById(productId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateLocalId(): String {
        return "local_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }

    suspend fun getProductCount(): Int {
        return productDao.countProducts()
    }

    suspend fun getUnSyncedProducts(): List<Product> {
        return productDao.unSyncedProducts().map { it.toDomainProduct() }
    }

    // Extension functions for data conversion
    private fun Product.toEntity(): ProductEntity {
        return ProductEntity(
            id = this.id,
            name = this.name,
            code = this.code,
            group_id = this.groupId.takeIf { it.isNotBlank() },
            brand_id = this.brandId.takeIf { it.isNotBlank() },
            category_id = this.categoryId.takeIf { it.isNotBlank() },
            sub_category_id = this.subCategoryId.takeIf { it.isNotBlank() },
            active = if (this.active) 1 else 0,
            tax_code = this.taxCode,
            mrp = this.mrp,
            dp = this.dp,
            selling_price = this.sellingPrice,
            base_unit = this.baseUnitId,
            last_updated = Clock.System.now().toEpochMilliseconds(),
            created_at = Clock.System.now().toString(),
            updated_at = Clock.System.now().toString(),
            synced = 0 // Mark as unsynced initially
        )
    }

    private fun ProductEntity.toDomainProduct(): Product {
        return Product(
            id = this.id,
            name = this.name,
            code = this.code,
            groupId = this.group_id ?: "",
            brandId = this.brand_id ?: "",
            categoryId = this.category_id ?: "",
            subCategoryId = this.sub_category_id ?: "",
            active = this.active == 1,
            taxCode = this.tax_code,
            mrp = this.mrp,
            dp = this.dp,
            sellingPrice = this.selling_price,
            baseUnitId = this.base_unit
        )
    }

    private fun ProductEntity.toProductListItem(): ProductListItem {
        return ProductListItem(
            id = this.id,
            name = this.name,
            code = this.code,
            mrp = this.mrp,
            sellingPrice = this.selling_price,
            categoryName = this.category_id, // TODO: Join with category table for actual name
            brandName = this.brand_id, // TODO: Join with brand table for actual name
            stockQuantity = null, // TODO: Add stock management
            imageUrl = null, // TODO: Join with image table
            active = this.active == 1
        )
    }
}