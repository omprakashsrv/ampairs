package com.ampairs.ecom.repository

import com.ampairs.ecom.domain.enums.StorefrontStatus
import com.ampairs.ecom.domain.model.Storefront
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface StorefrontRepository : CrudRepository<Storefront, Long> {
    fun findBySlug(slug: String): Storefront?
    fun findBySlugAndStatus(slug: String, status: StorefrontStatus): Storefront?
    fun existsBySlug(slug: String): Boolean
    fun findByOwnerId(ownerId: String): Storefront?
}
