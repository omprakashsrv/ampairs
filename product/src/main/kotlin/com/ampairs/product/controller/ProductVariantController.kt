package com.ampairs.product.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.product.domain.dto.ProductVariantRequest
import com.ampairs.product.domain.dto.ProductVariantResponse
import com.ampairs.product.service.ProductVariantService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.Instant

@RestController
@RequestMapping("/product/v1")
class ProductVariantController(
    private val variantService: ProductVariantService
) {

    /**
     * Get all variants for a product
     * GET /product/v1/{productId}/variants
     */
    @GetMapping("/{product_id}/variants")
    fun getProductVariants(@PathVariable("product_id") productId: String): ApiResponse<List<ProductVariantResponse>> {
        val variants = variantService.getProductVariants(productId)
        return ApiResponse.success(variants)
    }

    /**
     * Get a specific variant by UID
     * GET /product/v1/variants/{variantId}
     */
    @GetMapping("/variants/{variant_id}")
    fun getVariant(@PathVariable("variant_id") variantId: String): ApiResponse<ProductVariantResponse> {
        val variant = variantService.getVariant(variantId)
        return ApiResponse.success(variant)
    }

    /**
     * Get variant by SKU
     * GET /product/v1/variants/sku/{sku}
     */
    @GetMapping("/variants/sku/{sku}")
    fun getVariantBySku(@PathVariable sku: String): ApiResponse<ProductVariantResponse> {
        val variant = variantService.getVariantBySku(sku)
        return ApiResponse.success(variant)
    }

    /**
     * Create a new variant
     * POST /product/v1/{productId}/variants
     */
    @PostMapping("/{product_id}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    fun createVariant(
        @PathVariable("product_id") productId: String,
        @Valid @RequestBody request: ProductVariantRequest
    ): ApiResponse<ProductVariantResponse> {
        val variant = variantService.createVariant(productId, request)
        return ApiResponse.success(variant)
    }

    /**
     * Update a variant
     * PUT /product/v1/variants/{variantId}
     */
    @PutMapping("/variants/{variant_id}")
    fun updateVariant(
        @PathVariable("variant_id") variantId: String,
        @Valid @RequestBody request: ProductVariantRequest
    ): ApiResponse<ProductVariantResponse> {
        val variant = variantService.updateVariant(variantId, request)
        return ApiResponse.success(variant)
    }

    /**
     * Delete a variant (soft delete)
     * DELETE /product/v1/variants/{variantId}
     */
    @DeleteMapping("/variants/{variant_id}")
    fun deleteVariant(@PathVariable("variant_id") variantId: String): ApiResponse<String> {
        variantService.deleteVariant(variantId)
        return ApiResponse.success("Variant deleted successfully")
    }

    /**
     * Get total stock across all variants
     * GET /product/v1/{productId}/variants/stock
     */
    @GetMapping("/{product_id}/variants/stock")
    fun getTotalVariantStock(@PathVariable("product_id") productId: String): ApiResponse<BigDecimal> {
        val totalStock = variantService.getTotalVariantStock(productId)
        return ApiResponse.success(totalStock)
    }

    /**
     * Get available attribute options for a product
     * GET /product/v1/{productId}/variants/attributes
     */
    @GetMapping("/{product_id}/variants/attributes")
    fun getAttributeOptions(@PathVariable("product_id") productId: String): ApiResponse<Map<String, List<String>>> {
        val attributes = variantService.getAttributeOptions(productId)
        return ApiResponse.success(attributes)
    }

    /**
     * Sync: Get variants updated after timestamp
     * GET /product/v1/variants?updated_at=2025-01-01T00:00:00Z
     */
    @GetMapping("/variants")
    fun getVariantsForSync(
        @RequestParam(name = "updated_at", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        updatedAt: Instant?
    ): ApiResponse<List<ProductVariantResponse>> {
        val variants = if (updatedAt != null) {
            variantService.getVariantsUpdatedAfter(updatedAt)
        } else {
            emptyList()
        }
        return ApiResponse.success(variants)
    }
}
