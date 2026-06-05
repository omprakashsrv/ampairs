package com.ampairs.ecom.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.ecom.domain.dto.StorefrontRequest
import com.ampairs.ecom.domain.dto.StorefrontResponse
import com.ampairs.ecom.domain.dto.StorefrontUpdateRequest
import com.ampairs.ecom.domain.dto.TaxonomyImageRequest
import com.ampairs.ecom.domain.dto.TaxonomyImageResponse
import com.ampairs.ecom.domain.dto.asStorefrontResponse
import com.ampairs.ecom.domain.enums.TaxonomyType
import com.ampairs.ecom.domain.model.EcomTaxonomyImage
import com.ampairs.ecom.exception.EcomOrderNotFoundException
import com.ampairs.ecom.repository.EcomTaxonomyImageRepository
import com.ampairs.ecom.service.StorefrontService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.ResponseStatus

@RestController
@RequestMapping("/v1/ecom/management")
class StorefrontManagementController(
    private val storefrontService: StorefrontService,
    private val taxonomyImageRepository: EcomTaxonomyImageRepository,
) {

    @PostMapping("/storefront")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    fun createStorefront(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @RequestBody @Valid request: StorefrontRequest,
    ): ApiResponse<StorefrontResponse> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            return ApiResponse.success(storefrontService.createStorefront(request, workspaceId).asStorefrontResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @GetMapping("/storefront")
    @PreAuthorize("isAuthenticated()")
    fun getStorefront(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
    ): ApiResponse<StorefrontResponse> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            return ApiResponse.success(storefrontService.getStorefront(workspaceId).asStorefrontResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @PutMapping("/storefront")
    @PreAuthorize("isAuthenticated()")
    fun updateStorefront(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @RequestBody @Valid request: StorefrontUpdateRequest,
    ): ApiResponse<StorefrontResponse> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            return ApiResponse.success(storefrontService.updateStorefront(request, workspaceId).asStorefrontResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @PutMapping("/storefront/publish")
    @PreAuthorize("isAuthenticated()")
    fun publishStorefront(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
    ): ApiResponse<StorefrontResponse> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            return ApiResponse.success(storefrontService.publishStorefront(workspaceId).asStorefrontResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @PutMapping("/storefront/unpublish")
    @PreAuthorize("isAuthenticated()")
    fun unpublishStorefront(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
    ): ApiResponse<StorefrontResponse> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            return ApiResponse.success(storefrontService.unpublishStorefront(workspaceId).asStorefrontResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @GetMapping("/taxonomy")
    @PreAuthorize("isAuthenticated()")
    fun listTaxonomyImages(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @RequestParam type: TaxonomyType? = null,
    ): ApiResponse<List<TaxonomyImageResponse>> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            val storefront = storefrontService.getStorefront(workspaceId)
            val images = if (type != null) {
                taxonomyImageRepository.findByStorefrontIdAndTaxonomyTypeOrderBySortOrderAsc(storefront.uid, type)
            } else {
                taxonomyImageRepository.findByStorefrontIdOrderBySortOrderAsc(storefront.uid)
            }
            return ApiResponse.success(images.map { it.asTaxonomyImageResponse() })
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @PutMapping("/taxonomy")
    @PreAuthorize("isAuthenticated()")
    fun upsertTaxonomyImage(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @RequestBody @Valid request: TaxonomyImageRequest,
    ): ApiResponse<TaxonomyImageResponse> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            val storefront = storefrontService.getStorefront(workspaceId)
            val existing = taxonomyImageRepository.findByStorefrontIdAndTaxonomyTypeAndName(
                storefront.uid, request.type, request.name
            )
            val entity = existing ?: EcomTaxonomyImage().also {
                it.storefrontId = storefront.uid
                it.ownerId = workspaceId
                it.taxonomyType = request.type
                it.name = request.name
            }
            entity.imageUrl = request.imageUrl
            entity.sortOrder = request.sortOrder
            return ApiResponse.success(taxonomyImageRepository.save(entity).asTaxonomyImageResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @DeleteMapping("/taxonomy/{type}/{name}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTaxonomyImage(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @PathVariable type: TaxonomyType,
        @PathVariable name: String,
    ): ApiResponse<Unit> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            val storefront = storefrontService.getStorefront(workspaceId)
            val entity = taxonomyImageRepository.findByStorefrontIdAndTaxonomyTypeAndName(storefront.uid, type, name)
                ?: throw EcomOrderNotFoundException("Taxonomy image not found: $type/$name")
            taxonomyImageRepository.delete(entity)
            return ApiResponse.success(Unit)
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }
}

private fun EcomTaxonomyImage.asTaxonomyImageResponse() = TaxonomyImageResponse(
    uid = uid,
    type = taxonomyType,
    name = name,
    imageUrl = imageUrl,
    sortOrder = sortOrder,
)
