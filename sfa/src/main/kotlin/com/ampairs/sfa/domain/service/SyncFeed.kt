package com.ampairs.sfa.domain.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Shared offline-sync incremental feed: blank/invalid `last_sync` ⇒ all rows (paginated);
 * otherwise rows with `updatedAt >= last_sync`, INCLUDING soft-deleted ones (the repo query
 * does not filter on `active`). Mirrors the customer-module pattern.
 */
internal fun <T : Any> syncFeed(
    lastSync: String?,
    pageable: Pageable,
    findAll: (Pageable) -> Page<T>,
    findAfter: (Instant, Pageable) -> Page<T>,
): Page<T> {
    if (lastSync.isNullOrBlank()) return findAll(pageable)
    return try {
        val instant = Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8))
        findAfter(instant, pageable)
    } catch (e: Exception) {
        findAll(pageable)
    }
}
