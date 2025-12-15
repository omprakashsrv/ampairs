# Product Variant Backend Implementation Guide

## Overview
This guide provides step-by-step instructions to implement product variant support in the Ampairs backend product module. The mobile app already has full variant support implemented, and this backend implementation will complete the integration.

**Target Directory:** `/Users/omprakashsrv/IdeaProjects/ampairs/product`

---

## Current Backend Status

### ✅ Already Implemented
- Core Product CRUD operations
- Product metadata (Groups, Brands, Categories)
- Inventory management
- Tax code integration
- Product images
- Search and filtering
- Event publishing system

### ❌ Missing (To Be Implemented)
- Product variant entities and tables
- Product classification (ProductType, ServiceType)
- Variant management endpoints
- Variant-level inventory
- Variant-specific pricing

---

## Implementation Plan

### Phase 1: Database Schema (Migration)
### Phase 2: Domain Models (Entities)
### Phase 3: DTOs (Request/Response)
### Phase 4: Repositories
### Phase 5: Service Layer
### Phase 6: REST Controllers
### Phase 7: Testing & Integration

---

## Phase 1: Database Migration

**File:** `src/main/resources/db/migration/V1.0.22__add_product_variant_support.sql`

```sql
-- Add product classification columns to product table
ALTER TABLE product
ADD COLUMN product_type VARCHAR(50),
ADD COLUMN service_type VARCHAR(50),
ADD COLUMN has_variants BOOLEAN DEFAULT FALSE NOT NULL;

-- Add index for product_type filtering
CREATE INDEX idx_product_product_type ON product(product_type);
CREATE INDEX idx_product_has_variants ON product(has_variants);

-- Create product_variant table
CREATE TABLE product_variant (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(255) NOT NULL UNIQUE,
    product_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    variant_name VARCHAR(255) NOT NULL,

    -- Variant attributes (flexible key-value pairs)
    attribute_1_name VARCHAR(100),
    attribute_1_value VARCHAR(255),
    attribute_2_name VARCHAR(100),
    attribute_2_value VARCHAR(255),
    attribute_3_name VARCHAR(100),
    attribute_3_value VARCHAR(255),

    -- Pricing (can override product-level pricing)
    mrp DECIMAL(15,2),
    dp DECIMAL(15,2),
    selling_price DECIMAL(15,2),

    -- Stock management
    stock_quantity DECIMAL(15,3) DEFAULT 0 NOT NULL,
    low_stock_alert DECIMAL(15,3),

    -- Status
    active BOOLEAN DEFAULT TRUE NOT NULL,
    synced BOOLEAN DEFAULT FALSE NOT NULL,

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key
    CONSTRAINT fk_product_variant_product FOREIGN KEY (product_id)
        REFERENCES product(id) ON DELETE CASCADE
);

-- Create indices for product_variant
CREATE INDEX idx_product_variant_product_id ON product_variant(product_id);
CREATE INDEX idx_product_variant_sku ON product_variant(sku);
CREATE INDEX idx_product_variant_active ON product_variant(active);
CREATE INDEX idx_product_variant_updated_at ON product_variant(updated_at);

-- Create searchable variant attributes table (for advanced queries)
CREATE TABLE variant_attribute (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    attribute_name VARCHAR(100) NOT NULL,
    attribute_value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_variant_attribute_product FOREIGN KEY (product_id)
        REFERENCES product(id) ON DELETE CASCADE,
    CONSTRAINT uq_variant_attribute UNIQUE (product_id, attribute_name, attribute_value)
);

-- Create indices for variant_attribute
CREATE INDEX idx_variant_attribute_product_id ON variant_attribute(product_id);
CREATE INDEX idx_variant_attribute_name ON variant_attribute(attribute_name);
CREATE INDEX idx_variant_attribute_value ON variant_attribute(attribute_value);

-- Update existing products to have default product_type
UPDATE product SET product_type = 'RETAIL' WHERE product_type IS NULL;
```

---

## Phase 2: Domain Models (Entities)

### 2.1 Product Classification Enum

**File:** `src/main/kotlin/com/ampairs/product/domain/model/ProductType.kt`

```kotlin
package com.ampairs.product.domain.model

enum class ProductType {
    RETAIL,      // Physical retail products
    WHOLESALE,   // Bulk/wholesale items
    SERVICE;     // Service-based offerings

    companion object {
        fun fromString(value: String?): ProductType? {
            return value?.let {
                try {
                    valueOf(it.uppercase())
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
    }
}

enum class ServiceType {
    PHYSICAL,    // Physical goods
    SERVICE,     // Pure service
    DIGITAL;     // Digital products/services

    companion object {
        fun fromString(value: String?): ServiceType? {
            return value?.let {
                try {
                    valueOf(it.uppercase())
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
    }
}
```

### 2.2 Update Product Entity

**File:** `src/main/kotlin/com/ampairs/product/domain/model/Product.kt`

Add these fields to the existing Product entity:

```kotlin
// Add these imports
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.OneToMany
import javax.persistence.CascadeType

// Add these fields to the Product class
@Enumerated(EnumType.STRING)
@Column(name = "product_type", length = 50)
var productType: ProductType? = null

@Enumerated(EnumType.STRING)
@Column(name = "service_type", length = 50)
var serviceType: ServiceType? = null

@Column(name = "has_variants", nullable = false)
var hasVariants: Boolean = false

@OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true)
var variants: MutableList<ProductVariant> = mutableListOf()
```

### 2.3 ProductVariant Entity

**File:** `src/main/kotlin/com/ampairs/product/domain/model/ProductVariant.kt`

```kotlin
package com.ampairs.product.domain.model

import com.ampairs.common.domain.BaseDomain
import java.math.BigDecimal
import javax.persistence.*

@Entity
@Table(
    name = "product_variant",
    indexes = [
        Index(name = "idx_product_variant_product_id", columnList = "product_id"),
        Index(name = "idx_product_variant_sku", columnList = "sku"),
        Index(name = "idx_product_variant_active", columnList = "active"),
        Index(name = "idx_product_variant_updated_at", columnList = "updated_at")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uq_product_variant_uid", columnNames = ["uid"]),
        UniqueConstraint(name = "uq_product_variant_sku", columnNames = ["sku"])
    ]
)
data class ProductVariant(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var uid: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product? = null,

    @Column(nullable = false, unique = true, length = 100)
    var sku: String = "",

    @Column(name = "variant_name", nullable = false)
    var variantName: String = "",

    // Flexible attributes (up to 3 key-value pairs)
    @Column(name = "attribute_1_name", length = 100)
    var attribute1Name: String? = null,

    @Column(name = "attribute_1_value")
    var attribute1Value: String? = null,

    @Column(name = "attribute_2_name", length = 100)
    var attribute2Name: String? = null,

    @Column(name = "attribute_2_value")
    var attribute2Value: String? = null,

    @Column(name = "attribute_3_name", length = 100)
    var attribute3Name: String? = null,

    @Column(name = "attribute_3_value")
    var attribute3Value: String? = null,

    // Pricing (optional overrides)
    @Column(precision = 15, scale = 2)
    var mrp: BigDecimal? = null,

    @Column(precision = 15, scale = 2)
    var dp: BigDecimal? = null,

    @Column(name = "selling_price", precision = 15, scale = 2)
    var sellingPrice: BigDecimal? = null,

    // Stock management
    @Column(name = "stock_quantity", precision = 15, scale = 3, nullable = false)
    var stockQuantity: BigDecimal = BigDecimal.ZERO,

    @Column(name = "low_stock_alert", precision = 15, scale = 3)
    var lowStockAlert: BigDecimal? = null,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(nullable = false)
    var synced: Boolean = false

) : BaseDomain() {

    /**
     * Computed display name combining all attribute values
     */
    val displayName: String
        get() {
            val parts = mutableListOf<String>()
            attribute1Value?.let { parts.add(it) }
            attribute2Value?.let { parts.add(it) }
            attribute3Value?.let { parts.add(it) }
            return if (parts.isNotEmpty()) parts.joinToString(" - ") else variantName
        }

    /**
     * Check if variant is low on stock
     */
    val isLowStock: Boolean
        get() = lowStockAlert != null && stockQuantity <= lowStockAlert!!

    companion object {
        const val VARIANT_PREFIX = "VAR"
    }
}
```

### 2.4 VariantAttribute Entity

**File:** `src/main/kotlin/com/ampairs/product/domain/model/VariantAttribute.kt`

```kotlin
package com.ampairs.product.domain.model

import com.ampairs.common.domain.BaseDomain
import javax.persistence.*

@Entity
@Table(
    name = "variant_attribute",
    indexes = [
        Index(name = "idx_variant_attribute_product_id", columnList = "product_id"),
        Index(name = "idx_variant_attribute_name", columnList = "attribute_name"),
        Index(name = "idx_variant_attribute_value", columnList = "attribute_value")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_variant_attribute",
            columnNames = ["product_id", "attribute_name", "attribute_value"]
        )
    ]
)
data class VariantAttribute(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product? = null,

    @Column(name = "attribute_name", nullable = false, length = 100)
    var attributeName: String = "",

    @Column(name = "attribute_value", nullable = false)
    var attributeValue: String = ""

) : BaseDomain()
```

---

## Phase 3: DTOs (Request/Response)

### 3.1 ProductVariantRequest

**File:** `src/main/kotlin/com/ampairs/product/domain/dto/ProductVariantRequest.kt`

```kotlin
package com.ampairs.product.domain.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import javax.validation.constraints.NotBlank
import javax.validation.constraints.PositiveOrZero

data class ProductVariantRequest(
    @field:NotBlank(message = "Variant name is required")
    @JsonProperty("variant_name")
    val variantName: String,

    @JsonProperty("sku")
    val sku: String? = null, // Auto-generated if not provided

    // Variant attributes
    @JsonProperty("attribute_1_name")
    val attribute1Name: String? = null,

    @JsonProperty("attribute_1_value")
    val attribute1Value: String? = null,

    @JsonProperty("attribute_2_name")
    val attribute2Name: String? = null,

    @JsonProperty("attribute_2_value")
    val attribute2Value: String? = null,

    @JsonProperty("attribute_3_name")
    val attribute3Name: String? = null,

    @JsonProperty("attribute_3_value")
    val attribute3Value: String? = null,

    // Pricing (optional overrides)
    @field:PositiveOrZero(message = "MRP must be positive")
    @JsonProperty("mrp")
    val mrp: BigDecimal? = null,

    @field:PositiveOrZero(message = "Dealer price must be positive")
    @JsonProperty("dp")
    val dp: BigDecimal? = null,

    @field:PositiveOrZero(message = "Selling price must be positive")
    @JsonProperty("selling_price")
    val sellingPrice: BigDecimal? = null,

    // Stock
    @field:PositiveOrZero(message = "Stock quantity must be positive")
    @JsonProperty("stock_quantity")
    val stockQuantity: BigDecimal = BigDecimal.ZERO,

    @field:PositiveOrZero(message = "Low stock alert must be positive")
    @JsonProperty("low_stock_alert")
    val lowStockAlert: BigDecimal? = null,

    @JsonProperty("active")
    val active: Boolean = true
)
```

### 3.2 ProductVariantResponse

**File:** `src/main/kotlin/com/ampairs/product/domain/dto/ProductVariantResponse.kt`

```kotlin
package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.ProductVariant
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

data class ProductVariantResponse(
    @JsonProperty("uid")
    val uid: String,

    @JsonProperty("product_id")
    val productId: String,

    @JsonProperty("sku")
    val sku: String,

    @JsonProperty("variant_name")
    val variantName: String,

    @JsonProperty("display_name")
    val displayName: String,

    // Attributes
    @JsonProperty("attribute_1_name")
    val attribute1Name: String?,

    @JsonProperty("attribute_1_value")
    val attribute1Value: String?,

    @JsonProperty("attribute_2_name")
    val attribute2Name: String?,

    @JsonProperty("attribute_2_value")
    val attribute2Value: String?,

    @JsonProperty("attribute_3_name")
    val attribute3Name: String?,

    @JsonProperty("attribute_3_value")
    val attribute3Value: String?,

    // Pricing
    @JsonProperty("mrp")
    val mrp: BigDecimal?,

    @JsonProperty("dp")
    val dp: BigDecimal?,

    @JsonProperty("selling_price")
    val sellingPrice: BigDecimal?,

    // Stock
    @JsonProperty("stock_quantity")
    val stockQuantity: BigDecimal,

    @JsonProperty("low_stock_alert")
    val lowStockAlert: BigDecimal?,

    @JsonProperty("is_low_stock")
    val isLowStock: Boolean,

    @JsonProperty("active")
    val active: Boolean,

    @JsonProperty("synced")
    val synced: Boolean,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime?,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun fromEntity(variant: ProductVariant): ProductVariantResponse {
            return ProductVariantResponse(
                uid = variant.uid,
                productId = variant.product?.uid ?: "",
                sku = variant.sku,
                variantName = variant.variantName,
                displayName = variant.displayName,
                attribute1Name = variant.attribute1Name,
                attribute1Value = variant.attribute1Value,
                attribute2Name = variant.attribute2Name,
                attribute2Value = variant.attribute2Value,
                attribute3Name = variant.attribute3Name,
                attribute3Value = variant.attribute3Value,
                mrp = variant.mrp,
                dp = variant.dp,
                sellingPrice = variant.sellingPrice,
                stockQuantity = variant.stockQuantity,
                lowStockAlert = variant.lowStockAlert,
                isLowStock = variant.isLowStock,
                active = variant.active,
                synced = variant.synced,
                createdAt = variant.createdAt,
                updatedAt = variant.updatedAt
            )
        }
    }
}
```

### 3.3 Update ProductResponse

**File:** `src/main/kotlin/com/ampairs/product/domain/dto/ProductResponse.kt`

Add these fields to the existing ProductResponse:

```kotlin
@JsonProperty("product_type")
val productType: String?,

@JsonProperty("service_type")
val serviceType: String?,

@JsonProperty("has_variants")
val hasVariants: Boolean,

@JsonProperty("variants")
val variants: List<ProductVariantResponse>?
```

And update the `fromEntity` companion method:

```kotlin
productType = product.productType?.name,
serviceType = product.serviceType?.name,
hasVariants = product.hasVariants,
variants = product.variants.map { ProductVariantResponse.fromEntity(it) }
```

---

## Phase 4: Repositories

### 4.1 ProductVariantRepository

**File:** `src/main/kotlin/com/ampairs/product/repository/ProductVariantRepository.kt`

```kotlin
package com.ampairs.product.repository

import com.ampairs.product.domain.model.ProductVariant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ProductVariantRepository : JpaRepository<ProductVariant, Long> {

    fun findByUid(uid: String): ProductVariant?

    fun findBySku(sku: String): ProductVariant?

    @Query("SELECT v FROM ProductVariant v WHERE v.product.id = :productId AND v.active = true ORDER BY v.variantName")
    fun findActiveVariantsByProductId(@Param("productId") productId: Long): List<ProductVariant>

    @Query("SELECT v FROM ProductVariant v WHERE v.product.uid = :productUid AND v.active = true ORDER BY v.variantName")
    fun findActiveVariantsByProductUid(@Param("productUid") productUid: String): List<ProductVariant>

    @Query("SELECT v FROM ProductVariant v WHERE v.product.id = :productId ORDER BY v.variantName")
    fun findAllVariantsByProductId(@Param("productId") productId: Long): List<ProductVariant>

    @Query("SELECT SUM(v.stockQuantity) FROM ProductVariant v WHERE v.product.id = :productId AND v.active = true")
    fun getTotalStockByProductId(@Param("productId") productId: Long): Double?

    @Query("SELECT v FROM ProductVariant v WHERE v.updatedAt >= :timestamp")
    fun findVariantsUpdatedAfter(@Param("timestamp") timestamp: LocalDateTime): List<ProductVariant>

    @Query("SELECT v FROM ProductVariant v WHERE v.synced = false")
    fun findUnsyncedVariants(): List<ProductVariant>

    fun deleteByUid(uid: String)
}
```

### 4.2 VariantAttributeRepository

**File:** `src/main/kotlin/com/ampairs/product/repository/VariantAttributeRepository.kt`

```kotlin
package com.ampairs.product.repository

import com.ampairs.product.domain.model.VariantAttribute
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface VariantAttributeRepository : JpaRepository<VariantAttribute, Long> {

    @Query("SELECT DISTINCT va.attributeValue FROM VariantAttribute va WHERE va.product.id = :productId AND va.attributeName = :attributeName")
    fun findAttributeValuesByProductIdAndName(
        @Param("productId") productId: Long,
        @Param("attributeName") attributeName: String
    ): List<String>

    @Query("SELECT DISTINCT va.attributeName FROM VariantAttribute va WHERE va.product.id = :productId")
    fun findAttributeNamesByProductId(@Param("productId") productId: Long): List<String>

    fun deleteByProductId(productId: Long)
}
```

---

## Phase 5: Service Layer

### 5.1 ProductVariantService

**File:** `src/main/kotlin/com/ampairs/product/service/ProductVariantService.kt`

```kotlin
package com.ampairs.product.service

import com.ampairs.common.exception.ResourceNotFoundException
import com.ampairs.common.util.UidGenerator
import com.ampairs.product.domain.dto.ProductVariantRequest
import com.ampairs.product.domain.dto.ProductVariantResponse
import com.ampairs.product.domain.model.ProductVariant
import com.ampairs.product.domain.model.VariantAttribute
import com.ampairs.product.repository.ProductRepository
import com.ampairs.product.repository.ProductVariantRepository
import com.ampairs.product.repository.VariantAttributeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ProductVariantService(
    private val productRepository: ProductRepository,
    private val variantRepository: ProductVariantRepository,
    private val attributeRepository: VariantAttributeRepository
) {

    /**
     * Get all variants for a product
     */
    @Transactional(readOnly = true)
    fun getProductVariants(productUid: String): List<ProductVariantResponse> {
        return variantRepository.findActiveVariantsByProductUid(productUid)
            .map { ProductVariantResponse.fromEntity(it) }
    }

    /**
     * Get a specific variant by UID
     */
    @Transactional(readOnly = true)
    fun getVariant(variantUid: String): ProductVariantResponse {
        val variant = variantRepository.findByUid(variantUid)
            ?: throw ResourceNotFoundException("Variant not found: $variantUid")
        return ProductVariantResponse.fromEntity(variant)
    }

    /**
     * Get variant by SKU
     */
    @Transactional(readOnly = true)
    fun getVariantBySku(sku: String): ProductVariantResponse {
        val variant = variantRepository.findBySku(sku)
            ?: throw ResourceNotFoundException("Variant not found with SKU: $sku")
        return ProductVariantResponse.fromEntity(variant)
    }

    /**
     * Create a new variant
     */
    @Transactional
    fun createVariant(productUid: String, request: ProductVariantRequest): ProductVariantResponse {
        val product = productRepository.findByUid(productUid)
            ?: throw ResourceNotFoundException("Product not found: $productUid")

        // Ensure product has variants enabled
        if (!product.hasVariants) {
            product.hasVariants = true
            productRepository.save(product)
        }

        // Generate UID and SKU
        val uid = UidGenerator.generateUid(ProductVariant.VARIANT_PREFIX)
        val sku = request.sku ?: generateVariantSku(product.sku ?: product.code, uid)

        // Create variant entity
        val variant = ProductVariant(
            uid = uid,
            product = product,
            sku = sku,
            variantName = request.variantName,
            attribute1Name = request.attribute1Name,
            attribute1Value = request.attribute1Value,
            attribute2Name = request.attribute2Name,
            attribute2Value = request.attribute2Value,
            attribute3Name = request.attribute3Name,
            attribute3Value = request.attribute3Value,
            mrp = request.mrp,
            dp = request.dp,
            sellingPrice = request.sellingPrice,
            stockQuantity = request.stockQuantity,
            lowStockAlert = request.lowStockAlert,
            active = request.active
        )

        val savedVariant = variantRepository.save(variant)

        // Create searchable attribute entries
        saveVariantAttributes(product.id!!, request)

        return ProductVariantResponse.fromEntity(savedVariant)
    }

    /**
     * Update an existing variant
     */
    @Transactional
    fun updateVariant(variantUid: String, request: ProductVariantRequest): ProductVariantResponse {
        val variant = variantRepository.findByUid(variantUid)
            ?: throw ResourceNotFoundException("Variant not found: $variantUid")

        // Update fields
        variant.variantName = request.variantName
        variant.attribute1Name = request.attribute1Name
        variant.attribute1Value = request.attribute1Value
        variant.attribute2Name = request.attribute2Name
        variant.attribute2Value = request.attribute2Value
        variant.attribute3Name = request.attribute3Name
        variant.attribute3Value = request.attribute3Value
        variant.mrp = request.mrp
        variant.dp = request.dp
        variant.sellingPrice = request.sellingPrice
        variant.stockQuantity = request.stockQuantity
        variant.lowStockAlert = request.lowStockAlert
        variant.active = request.active
        variant.synced = false // Mark for sync

        val updatedVariant = variantRepository.save(variant)

        // Update searchable attributes
        saveVariantAttributes(variant.product!!.id!!, request)

        return ProductVariantResponse.fromEntity(updatedVariant)
    }

    /**
     * Delete a variant
     */
    @Transactional
    fun deleteVariant(variantUid: String) {
        val variant = variantRepository.findByUid(variantUid)
            ?: throw ResourceNotFoundException("Variant not found: $variantUid")

        // Soft delete - mark as inactive
        variant.active = false
        variant.synced = false
        variantRepository.save(variant)

        // Alternatively, hard delete:
        // variantRepository.deleteByUid(variantUid)
    }

    /**
     * Get total stock across all variants
     */
    @Transactional(readOnly = true)
    fun getTotalVariantStock(productUid: String): Double {
        val product = productRepository.findByUid(productUid)
            ?: throw ResourceNotFoundException("Product not found: $productUid")
        return variantRepository.getTotalStockByProductId(product.id!!) ?: 0.0
    }

    /**
     * Get available attribute values for a product
     */
    @Transactional(readOnly = true)
    fun getAttributeOptions(productUid: String): Map<String, List<String>> {
        val product = productRepository.findByUid(productUid)
            ?: throw ResourceNotFoundException("Product not found: $productUid")

        val attributeNames = attributeRepository.findAttributeNamesByProductId(product.id!!)

        return attributeNames.associateWith { name ->
            attributeRepository.findAttributeValuesByProductIdAndName(product.id!!, name)
        }
    }

    /**
     * Sync variants updated after timestamp
     */
    @Transactional(readOnly = true)
    fun getVariantsUpdatedAfter(timestamp: LocalDateTime): List<ProductVariantResponse> {
        return variantRepository.findVariantsUpdatedAfter(timestamp)
            .map { ProductVariantResponse.fromEntity(it) }
    }

    // Private helper methods

    private fun generateVariantSku(productSku: String, variantUid: String): String {
        // Generate unique SKU: PRODUCT_SKU-VARIANT_ID
        return "${productSku}-${variantUid.takeLast(8)}"
    }

    private fun saveVariantAttributes(productId: Long, request: ProductVariantRequest) {
        val attributes = mutableListOf<VariantAttribute>()

        request.attribute1Name?.let { name ->
            request.attribute1Value?.let { value ->
                attributes.add(VariantAttribute(
                    product = productRepository.findById(productId).orElse(null),
                    attributeName = name,
                    attributeValue = value
                ))
            }
        }

        request.attribute2Name?.let { name ->
            request.attribute2Value?.let { value ->
                attributes.add(VariantAttribute(
                    product = productRepository.findById(productId).orElse(null),
                    attributeName = name,
                    attributeValue = value
                ))
            }
        }

        request.attribute3Name?.let { name ->
            request.attribute3Value?.let { value ->
                attributes.add(VariantAttribute(
                    product = productRepository.findById(productId).orElse(null),
                    attributeName = name,
                    attributeValue = value
                ))
            }
        }

        if (attributes.isNotEmpty()) {
            attributeRepository.saveAll(attributes)
        }
    }
}
```

---

## Phase 6: REST Controllers

### 6.1 ProductVariantController

**File:** `src/main/kotlin/com/ampairs/product/controller/ProductVariantController.kt`

```kotlin
package com.ampairs.product.controller

import com.ampairs.common.response.ApiResponse
import com.ampairs.product.domain.dto.ProductVariantRequest
import com.ampairs.product.domain.dto.ProductVariantResponse
import com.ampairs.product.service.ProductVariantService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import javax.validation.Valid

@RestController
@RequestMapping("/product/v1")
class ProductVariantController(
    private val variantService: ProductVariantService
) {

    /**
     * Get all variants for a product
     * GET /product/v1/{productId}/variants
     */
    @GetMapping("/{productId}/variants")
    fun getProductVariants(@PathVariable productId: String): ResponseEntity<ApiResponse<List<ProductVariantResponse>>> {
        val variants = variantService.getProductVariants(productId)
        return ResponseEntity.ok(ApiResponse.success(variants))
    }

    /**
     * Get a specific variant by UID
     * GET /product/v1/variants/{variantId}
     */
    @GetMapping("/variants/{variantId}")
    fun getVariant(@PathVariable variantId: String): ResponseEntity<ApiResponse<ProductVariantResponse>> {
        val variant = variantService.getVariant(variantId)
        return ResponseEntity.ok(ApiResponse.success(variant))
    }

    /**
     * Get variant by SKU
     * GET /product/v1/variants/sku/{sku}
     */
    @GetMapping("/variants/sku/{sku}")
    fun getVariantBySku(@PathVariable sku: String): ResponseEntity<ApiResponse<ProductVariantResponse>> {
        val variant = variantService.getVariantBySku(sku)
        return ResponseEntity.ok(ApiResponse.success(variant))
    }

    /**
     * Create a new variant
     * POST /product/v1/{productId}/variants
     */
    @PostMapping("/{productId}/variants")
    fun createVariant(
        @PathVariable productId: String,
        @Valid @RequestBody request: ProductVariantRequest
    ): ResponseEntity<ApiResponse<ProductVariantResponse>> {
        val variant = variantService.createVariant(productId, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(variant))
    }

    /**
     * Update a variant
     * PUT /product/v1/variants/{variantId}
     */
    @PutMapping("/variants/{variantId}")
    fun updateVariant(
        @PathVariable variantId: String,
        @Valid @RequestBody request: ProductVariantRequest
    ): ResponseEntity<ApiResponse<ProductVariantResponse>> {
        val variant = variantService.updateVariant(variantId, request)
        return ResponseEntity.ok(ApiResponse.success(variant))
    }

    /**
     * Delete a variant (soft delete)
     * DELETE /product/v1/variants/{variantId}
     */
    @DeleteMapping("/variants/{variantId}")
    fun deleteVariant(@PathVariable variantId: String): ResponseEntity<ApiResponse<String>> {
        variantService.deleteVariant(variantId)
        return ResponseEntity.ok(ApiResponse.success("Variant deleted successfully"))
    }

    /**
     * Get total stock across all variants
     * GET /product/v1/{productId}/variants/stock
     */
    @GetMapping("/{productId}/variants/stock")
    fun getTotalVariantStock(@PathVariable productId: String): ResponseEntity<ApiResponse<Double>> {
        val totalStock = variantService.getTotalVariantStock(productId)
        return ResponseEntity.ok(ApiResponse.success(totalStock))
    }

    /**
     * Get available attribute options for a product
     * GET /product/v1/{productId}/variants/attributes
     */
    @GetMapping("/{productId}/variants/attributes")
    fun getAttributeOptions(@PathVariable productId: String): ResponseEntity<ApiResponse<Map<String, List<String>>>> {
        val attributes = variantService.getAttributeOptions(productId)
        return ResponseEntity.ok(ApiResponse.success(attributes))
    }

    /**
     * Sync: Get variants updated after timestamp
     * GET /product/v1/variants?updated_at=2025-01-01T00:00:00
     */
    @GetMapping("/variants")
    fun getVariantsForSync(
        @RequestParam(name = "updated_at", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        updatedAt: LocalDateTime?
    ): ResponseEntity<ApiResponse<List<ProductVariantResponse>>> {
        val variants = if (updatedAt != null) {
            variantService.getVariantsUpdatedAfter(updatedAt)
        } else {
            emptyList()
        }
        return ResponseEntity.ok(ApiResponse.success(variants))
    }
}
```

### 6.2 Update ProductController

**File:** `src/main/kotlin/com/ampairs/product/controller/ProductController.kt`

Update the ProductResponse to include variant data when fetching product details. The existing controller endpoints should automatically include variants if the ProductResponse DTO is updated correctly in Phase 3.

---

## Phase 7: Testing

### 7.1 Unit Tests

**File:** `src/test/kotlin/com/ampairs/product/service/ProductVariantServiceTest.kt`

```kotlin
package com.ampairs.product.service

import com.ampairs.product.domain.dto.ProductVariantRequest
import com.ampairs.product.domain.model.Product
import com.ampairs.product.domain.model.ProductVariant
import com.ampairs.product.repository.ProductRepository
import com.ampairs.product.repository.ProductVariantRepository
import com.ampairs.product.repository.VariantAttributeRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.math.BigDecimal

class ProductVariantServiceTest {

    private lateinit var productRepository: ProductRepository
    private lateinit var variantRepository: ProductVariantRepository
    private lateinit var attributeRepository: VariantAttributeRepository
    private lateinit var service: ProductVariantService

    @BeforeEach
    fun setup() {
        productRepository = mock()
        variantRepository = mock()
        attributeRepository = mock()
        service = ProductVariantService(productRepository, variantRepository, attributeRepository)
    }

    @Test
    fun `createVariant should create variant with generated UID and SKU`() {
        // Given
        val productUid = "PRD123"
        val product = Product(uid = productUid, code = "PROD", hasVariants = false)
        val request = ProductVariantRequest(
            variantName = "Red Large",
            attribute1Name = "Color",
            attribute1Value = "Red",
            attribute2Name = "Size",
            attribute2Value = "Large",
            mrp = BigDecimal("100.00"),
            sellingPrice = BigDecimal("80.00"),
            stockQuantity = BigDecimal("50")
        )

        whenever(productRepository.findByUid(productUid)).thenReturn(product)
        whenever(variantRepository.save(any<ProductVariant>())).thenAnswer { it.arguments[0] }

        // When
        val response = service.createVariant(productUid, request)

        // Then
        assertNotNull(response.uid)
        assertTrue(response.uid.startsWith("VAR"))
        assertEquals("Red Large", response.variantName)
        assertEquals("Red", response.attribute1Value)
        assertEquals("Large", response.attribute2Value)
        verify(productRepository).save(argThat { hasVariants })
    }

    @Test
    fun `getProductVariants should return all active variants`() {
        // Given
        val productUid = "PRD123"
        val variants = listOf(
            ProductVariant(uid = "VAR1", variantName = "Red", active = true),
            ProductVariant(uid = "VAR2", variantName = "Blue", active = true)
        )

        whenever(variantRepository.findActiveVariantsByProductUid(productUid)).thenReturn(variants)

        // When
        val responses = service.getProductVariants(productUid)

        // Then
        assertEquals(2, responses.size)
        assertEquals("VAR1", responses[0].uid)
        assertEquals("VAR2", responses[1].uid)
    }
}
```

### 7.2 Integration Tests

**File:** `src/test/kotlin/com/ampairs/product/controller/ProductVariantControllerIntegrationTest.kt`

```kotlin
package com.ampairs.product.controller

import com.ampairs.product.domain.dto.ProductVariantRequest
import com.ampairs.product.domain.model.Product
import com.ampairs.product.repository.ProductRepository
import com.ampairs.product.repository.ProductVariantRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class ProductVariantControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var variantRepository: ProductVariantRepository

    @Test
    fun `should create and retrieve product variant`() {
        // Create product
        val product = Product(
            uid = "PRD_TEST_001",
            name = "Test Product",
            code = "TEST",
            hasVariants = true
        )
        productRepository.save(product)

        // Create variant
        val request = ProductVariantRequest(
            variantName = "Red Large",
            attribute1Name = "Color",
            attribute1Value = "Red",
            attribute2Name = "Size",
            attribute2Value = "Large",
            mrp = BigDecimal("100.00"),
            sellingPrice = BigDecimal("80.00"),
            stockQuantity = BigDecimal("50")
        )

        mockMvc.perform(
            post("/product/v1/${product.uid}/variants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.variant_name").value("Red Large"))
            .andExpect(jsonPath("$.data.attribute_1_value").value("Red"))
    }
}
```

---

## Phase 8: Mobile API Integration

### 8.1 Update Mobile API Endpoints

Ensure mobile app calls these endpoints:

```
GET    /product/v1/{productId}/variants          - List variants
POST   /product/v1/{productId}/variants          - Create variant
GET    /product/v1/variants/{variantId}          - Get variant
PUT    /product/v1/variants/{variantId}          - Update variant
DELETE /product/v1/variants/{variantId}          - Delete variant
GET    /product/v1/variants?updated_at=TIMESTAMP - Sync variants
```

### 8.2 Sync Strategy

The mobile app should:
1. Poll `/product/v1/variants?updated_at=LAST_SYNC` periodically
2. Download new/updated variants
3. Mark local variants as `synced = true` after successful upload
4. Handle conflict resolution (server timestamp wins)

---

## Summary Checklist

### Database
- [ ] Create migration V1.0.22 with variant tables
- [ ] Add product_type, service_type, has_variants columns to product table
- [ ] Create indices for performance

### Domain Models
- [ ] Create ProductType and ServiceType enums
- [ ] Update Product entity with new fields
- [ ] Create ProductVariant entity
- [ ] Create VariantAttribute entity

### DTOs
- [ ] Create ProductVariantRequest
- [ ] Create ProductVariantResponse
- [ ] Update ProductResponse with variant fields

### Repositories
- [ ] Create ProductVariantRepository
- [ ] Create VariantAttributeRepository

### Services
- [ ] Create ProductVariantService with all CRUD methods
- [ ] Add variant SKU generation logic
- [ ] Add variant attribute management

### Controllers
- [ ] Create ProductVariantController with all REST endpoints
- [ ] Update ProductController to return variants in product details

### Testing
- [ ] Write unit tests for ProductVariantService
- [ ] Write integration tests for ProductVariantController
- [ ] Test mobile app synchronization

### Documentation
- [ ] Update API documentation with variant endpoints
- [ ] Update mobile integration guide

---

## Additional Notes

1. **UID Generation**: Use existing `UidGenerator.generateUid("VAR")` for variant UIDs
2. **SKU Generation**: Auto-generate as `{PRODUCT_SKU}-{VARIANT_ID_LAST_8}` if not provided
3. **Soft Delete**: Mark variants as `active = false` instead of hard delete for data integrity
4. **Sync Status**: Track `synced` flag for mobile synchronization
5. **Stock Aggregation**: Parent product total stock = sum of all variant stocks
6. **Pricing**: Variants can override product pricing (mrp, dp, sellingPrice) or inherit
7. **Validation**: Ensure at least one variant exists when `hasVariants = true`

---

## Reference Files

**Mobile Implementation:**
- `/Users/omprakashsrv/StudioProjects/ampairs-app/composeApp/src/commonMain/kotlin/com/ampairs/product/domain/ProductVariant.kt`
- `/Users/omprakashsrv/StudioProjects/ampairs-app/composeApp/src/commonMain/kotlin/com/ampairs/product/ui/variant/`

**Backend Base:**
- `/Users/omprakashsrv/IdeaProjects/ampairs/product/src/main/kotlin/com/ampairs/product/domain/model/Product.kt`
- `/Users/omprakashsrv/IdeaProjects/ampairs/product/src/main/kotlin/com/ampairs/product/service/ProductService.kt`

---

**Last Updated:** January 2025
**Mobile Version:** v3.0 (Full Variant Support)
**Backend Target:** v1.0.22+