package com.ampairs.ecom.domain.dto

import com.ampairs.ecom.domain.enums.StorefrontStatus
import com.ampairs.ecom.domain.model.Storefront
import org.springframework.data.domain.Page

/**
 * Compact storefront record for the public multi-store directory
 * (GET /api/v1/storefronts). The serialized snake_case keys (uid, slug, name,
 * description, logo_url, banner_url, status, brand_color_argb) are a load-bearing
 * contract with the mobile "common ecom app" — do NOT rename them.
 */
data class StorefrontSummary(
    val uid: String,
    val slug: String,
    val name: String,
    val description: String?,
    val logoUrl: String?,
    val bannerUrl: String?,
    val status: StorefrontStatus,
    val brandColorArgb: Long?,
)

fun Storefront.asStorefrontSummary() = StorefrontSummary(
    uid = uid,
    slug = slug,
    name = name,
    description = description,
    logoUrl = logoUrl,
    bannerUrl = bannerUrl,
    status = status,
    brandColorArgb = brandColorArgb,
)

/**
 * Pagination envelope for the storefront directory. Deliberately separate from the
 * shared [com.ampairs.core.domain.dto.PageResponse] (which serializes page_number/
 * page_size) because the mobile client is coded against these exact keys: content,
 * page, size, total_elements, total_pages, first, last (snake_case via the global
 * Jackson strategy).
 */
data class StorefrontDirectoryPage(
    val content: List<StorefrontSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
) {
    companion object {
        fun from(page: Page<StorefrontSummary>) = StorefrontDirectoryPage(
            content = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            first = page.isFirst,
            last = page.isLast,
        )
    }
}
