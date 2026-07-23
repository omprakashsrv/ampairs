package com.ampairs.ecom.repository

import com.ampairs.ecom.domain.model.Storefront
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface StorefrontRepository : CrudRepository<Storefront, Long> {

    // Slug is globally unique across tenants, so these lookups are cross-tenant by nature:
    // a customer of any/no tenant resolves a storefront by slug, and the StorefrontTenantInterceptor
    // then sets the tenant from the resolved owner. They MUST use nativeQuery to bypass the
    // @TenantId (owner_id) auto-filter — otherwise Hibernate appends `owner_id = <currentTenant>`
    // (which resolves to "default" when no tenant is set yet) and the published row is never found.
    @Query(value = "SELECT * FROM ecom_storefront WHERE slug = :slug LIMIT 1", nativeQuery = true)
    fun findBySlug(@Param("slug") slug: String): Storefront?

    @Query(
        value = "SELECT * FROM ecom_storefront WHERE slug = :slug AND status = :status LIMIT 1",
        nativeQuery = true,
    )
    fun findBySlugAndStatus(@Param("slug") slug: String, @Param("status") status: String): Storefront?

    @Query(value = "SELECT EXISTS(SELECT 1 FROM ecom_storefront WHERE slug = :slug)", nativeQuery = true)
    fun existsBySlug(@Param("slug") slug: String): Boolean

    fun findByOwnerId(ownerId: String): Storefront?

    // Public directory feed: published storefronts across ALL tenants, optionally filtered
    // by a case-insensitive substring of name or slug. Native (like the slug lookups above)
    // so the @TenantId owner_id auto-filter is bypassed — the directory is intentionally
    // cross-tenant. Callers pass an empty string to match every published row (LIKE '%%').
    @Query(
        value = """
            SELECT * FROM ecom_storefront
            WHERE status = 'PUBLISHED'
              AND (LOWER(name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(slug) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY name ASC
        """,
        countQuery = """
            SELECT COUNT(*) FROM ecom_storefront
            WHERE status = 'PUBLISHED'
              AND (LOWER(name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(slug) LIKE LOWER(CONCAT('%', :q, '%')))
        """,
        nativeQuery = true,
    )
    fun searchPublished(@Param("q") q: String, pageable: Pageable): Page<Storefront>
}
