package com.ampairs.ecom.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.ecom.domain.dto.StorefrontDirectoryPage
import com.ampairs.ecom.domain.dto.asStorefrontSummary
import com.ampairs.ecom.service.StorefrontService
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Public, cross-tenant storefront directory for the multi-store mobile app.
 *
 * Unlike the rest of the ecom API this endpoint is NOT workspace-scoped: it lists
 * PUBLISHED storefronts across every workspace so a customer can discover and pick one.
 * It requires neither the `X-Workspace-ID` header nor authentication — it is whitelisted
 * in `SecurityConfiguration.PUBLIC_PATHS` and skipped by `SessionUserFilter`. A bearer
 * token may be present (the app is login-first) but is not required. The backing query is
 * native and bypasses the `@TenantId` owner filter (see `StorefrontRepository`).
 *
 * Effective URL: `GET /api/v1/storefronts` (servlet path `/api`).
 */
@RestController
@RequestMapping("/v1/storefronts")
class StorefrontDirectoryController(
    private val storefrontService: StorefrontService,
) {

    @GetMapping
    fun listStorefronts(
        @RequestParam(name = "q", required = false) q: String? = null,
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
    ): ApiResponse<StorefrontDirectoryPage> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, MAX_PAGE_SIZE))
        val result = storefrontService.listPublishedStorefronts(q, pageable)
        return ApiResponse.success(StorefrontDirectoryPage.from(result.map { it.asStorefrontSummary() }))
    }

    companion object {
        private const val MAX_PAGE_SIZE = 50
    }
}
