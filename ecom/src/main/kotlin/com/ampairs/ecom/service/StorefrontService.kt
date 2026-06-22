package com.ampairs.ecom.service

import com.ampairs.core.service.EcomStorefrontLookupService
import com.ampairs.ecom.domain.dto.StorefrontRequest
import com.ampairs.ecom.domain.dto.StorefrontUpdateRequest
import com.ampairs.ecom.domain.dto.applyTo
import com.ampairs.ecom.domain.dto.toStorefront
import com.ampairs.ecom.domain.enums.StorefrontStatus
import com.ampairs.ecom.domain.model.Storefront
import com.ampairs.ecom.exception.StorefrontAlreadyExistsException
import com.ampairs.ecom.exception.StorefrontNotFoundException
import com.ampairs.ecom.exception.StorefrontSlugConflictException
import com.ampairs.ecom.repository.StorefrontRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class StorefrontService(
    private val storefrontRepository: StorefrontRepository,
) : EcomStorefrontLookupService {

    @Transactional
    fun createStorefront(request: StorefrontRequest, workspaceId: String): Storefront {
        if (storefrontRepository.existsBySlug(request.slug)) {
            throw StorefrontSlugConflictException("Slug '${request.slug}' is already taken")
        }
        storefrontRepository.findByOwnerId(workspaceId)?.let {
            throw StorefrontAlreadyExistsException("Workspace already has a storefront")
        }
        return storefrontRepository.save(request.toStorefront(workspaceId))
    }

    @Transactional
    fun updateStorefront(request: StorefrontUpdateRequest, workspaceId: String): Storefront {
        val storefront = getStorefront(workspaceId)
        return storefrontRepository.save(request.applyTo(storefront))
    }

    @Transactional(readOnly = true)
    fun getStorefront(workspaceId: String): Storefront =
        storefrontRepository.findByOwnerId(workspaceId)
            ?: throw StorefrontNotFoundException("No storefront found for workspace $workspaceId")

    @Transactional(readOnly = true)
    fun getPublishedStorefrontBySlug(slug: String): Storefront =
        storefrontRepository.findBySlugAndStatus(slug, StorefrontStatus.PUBLISHED.name)
            ?: throw StorefrontNotFoundException("No published storefront found with slug '$slug'")

    @Transactional
    fun publishStorefront(workspaceId: String): Storefront {
        val storefront = getStorefront(workspaceId)
        storefront.status = StorefrontStatus.PUBLISHED
        storefront.publishedAt = Instant.now()
        storefront.unpublishedAt = null
        return storefrontRepository.save(storefront)
    }

    @Transactional
    fun unpublishStorefront(workspaceId: String): Storefront {
        val storefront = getStorefront(workspaceId)
        storefront.status = StorefrontStatus.UNPUBLISHED
        storefront.unpublishedAt = Instant.now()
        return storefrontRepository.save(storefront)
    }

    override fun findStorefrontIdByWorkspaceId(workspaceId: String): String? =
        storefrontRepository.findByOwnerId(workspaceId)?.uid
}
