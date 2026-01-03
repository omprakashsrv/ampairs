package com.ampairs.product.service

import com.ampairs.core.exception.NotFoundException
import com.ampairs.product.domain.dto.ProductVariantRequest
import com.ampairs.product.domain.dto.ProductVariantResponse
import com.ampairs.product.domain.dto.asResponse
import com.ampairs.product.domain.model.ProductVariant
import com.ampairs.product.domain.model.VariantAttribute
import com.ampairs.product.repository.ProductRepository
import com.ampairs.product.repository.ProductVariantRepository
import com.ampairs.product.repository.VariantAttributeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

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
        return variantRepository.findActiveVariantsByProductId(productUid).asResponse()
    }

    /**
     * Get a specific variant by UID
     */
    @Transactional(readOnly = true)
    fun getVariant(variantUid: String): ProductVariantResponse {
        val variant = variantRepository.findByUid(variantUid)
            ?: throw NotFoundException("Variant not found: $variantUid")
        return variant.asResponse()
    }

    /**
     * Get variant by SKU
     */
    @Transactional(readOnly = true)
    fun getVariantBySku(sku: String): ProductVariantResponse {
        val variant = variantRepository.findBySku(sku)
            ?: throw NotFoundException("Variant not found with SKU: $sku")
        return variant.asResponse()
    }

    /**
     * Create a new variant
     */
    @Transactional
    fun createVariant(productUid: String, request: ProductVariantRequest): ProductVariantResponse {
        val product = productRepository.findByUid(productUid)
            ?: throw NotFoundException("Product not found: $productUid")

        // Ensure product has variants enabled
        if (!product.hasVariants) {
            product.hasVariants = true
            productRepository.save(product)
        }

        // Generate SKU if not provided
        val sku = request.sku ?: generateVariantSku(product.sku, product.uid)

        // Create variant entity
        val variant = ProductVariant().apply {
            this.productId = product.uid
            this.sku = sku
            this.variantName = request.variantName
            this.attribute1Name = request.attribute1Name
            this.attribute1Value = request.attribute1Value
            this.attribute2Name = request.attribute2Name
            this.attribute2Value = request.attribute2Value
            this.attribute3Name = request.attribute3Name
            this.attribute3Value = request.attribute3Value
            this.mrp = request.mrp
            this.dp = request.dp
            this.sellingPrice = request.sellingPrice
            this.stockQuantity = request.stockQuantity
            this.lowStockAlert = request.lowStockAlert
            this.active = request.active
        }

        val savedVariant = variantRepository.save(variant)

        // Create searchable attribute entries
        saveVariantAttributes(product.uid, request)

        return savedVariant.asResponse()
    }

    /**
     * Update an existing variant
     */
    @Transactional
    fun updateVariant(variantUid: String, request: ProductVariantRequest): ProductVariantResponse {
        val variant = variantRepository.findByUid(variantUid)
            ?: throw NotFoundException("Variant not found: $variantUid")

        // Update fields
        variant.apply {
            this.variantName = request.variantName
            this.attribute1Name = request.attribute1Name
            this.attribute1Value = request.attribute1Value
            this.attribute2Name = request.attribute2Name
            this.attribute2Value = request.attribute2Value
            this.attribute3Name = request.attribute3Name
            this.attribute3Value = request.attribute3Value
            this.mrp = request.mrp
            this.dp = request.dp
            this.sellingPrice = request.sellingPrice
            this.stockQuantity = request.stockQuantity
            this.lowStockAlert = request.lowStockAlert
            this.active = request.active
            this.synced = false // Mark for sync
        }

        val updatedVariant = variantRepository.save(variant)

        // Update searchable attributes
        saveVariantAttributes(variant.productId, request)

        return updatedVariant.asResponse()
    }

    /**
     * Delete a variant (soft delete)
     */
    @Transactional
    fun deleteVariant(variantUid: String) {
        val variant = variantRepository.findByUid(variantUid)
            ?: throw NotFoundException("Variant not found: $variantUid")

        // Soft delete - mark as inactive
        variant.active = false
        variant.synced = false
        variantRepository.save(variant)
    }

    /**
     * Get total stock across all variants
     */
    @Transactional(readOnly = true)
    fun getTotalVariantStock(productUid: String): BigDecimal {
        val product = productRepository.findByUid(productUid)
            ?: throw NotFoundException("Product not found: $productUid")
        return variantRepository.getTotalStockByProductId(product.uid) ?: BigDecimal.ZERO
    }

    /**
     * Get available attribute options for a product
     */
    @Transactional(readOnly = true)
    fun getAttributeOptions(productUid: String): Map<String, List<String>> {
        val product = productRepository.findByUid(productUid)
            ?: throw NotFoundException("Product not found: $productUid")

        val attributeNames = attributeRepository.findAttributeNamesByProductId(product.uid)

        return attributeNames.associateWith { name ->
            attributeRepository.findAttributeValuesByProductIdAndName(product.uid, name)
        }
    }

    /**
     * Sync variants updated after timestamp
     */
    @Transactional(readOnly = true)
    fun getVariantsUpdatedAfter(timestamp: Instant): List<ProductVariantResponse> {
        return variantRepository.findVariantsUpdatedAfter(timestamp).asResponse()
    }

    // Private helper methods

    private fun generateVariantSku(productSku: String, productUid: String): String {
        // Generate unique SKU: PRODUCT_SKU-TIMESTAMP
        val timestamp = System.currentTimeMillis().toString().takeLast(8)
        return "${productSku}-${timestamp}"
    }

    private fun saveVariantAttributes(productId: String, request: ProductVariantRequest) {
        val attributes = mutableListOf<VariantAttribute>()

        request.attribute1Name?.let { name ->
            request.attribute1Value?.let { value ->
                attributes.add(VariantAttribute().apply {
                    this.productId = productId
                    this.attributeName = name
                    this.attributeValue = value
                })
            }
        }

        request.attribute2Name?.let { name ->
            request.attribute2Value?.let { value ->
                attributes.add(VariantAttribute().apply {
                    this.productId = productId
                    this.attributeName = name
                    this.attributeValue = value
                })
            }
        }

        request.attribute3Name?.let { name ->
            request.attribute3Value?.let { value ->
                attributes.add(VariantAttribute().apply {
                    this.productId = productId
                    this.attributeName = name
                    this.attributeValue = value
                })
            }
        }

        if (attributes.isNotEmpty()) {
            attributeRepository.saveAll(attributes)
        }
    }
}
