package com.ampairs.payment.service

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

/** Shared helpers for the canonical `/sync` feed paths. */
internal object SyncSupport {

    /**
     * Parses an optional, possibly URL-encoded ISO-8601 `last_sync` cursor.
     * Returns null when blank or unparseable (caller falls back to the full feed).
     */
    fun parseLastSync(lastSync: String?): Instant? {
        if (lastSync.isNullOrBlank()) return null
        return try {
            Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }
}
