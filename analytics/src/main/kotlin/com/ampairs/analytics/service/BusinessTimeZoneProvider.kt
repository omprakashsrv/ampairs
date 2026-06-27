package com.ampairs.analytics.service

import com.ampairs.business.service.BusinessService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Resolves the active workspace's business timezone (R7). All analytics day/week/month bucketing
 * MUST go through this — never the server/device default zone.
 *
 * Reads the `business` module's public [BusinessService] (cross-module access via public service
 * interface only — Principle IX). Falls back to UTC when no business profile or an invalid zone id
 * is configured, so bucketing never throws.
 */
@Service
class BusinessTimeZoneProvider(
    private val businessService: BusinessService,
) {
    private val log = LoggerFactory.getLogger(BusinessTimeZoneProvider::class.java)

    /** The current tenant's business [ZoneId], or [ZoneOffset.UTC] if unset/invalid. */
    fun currentZone(): ZoneId =
        try {
            val tz = businessService.getBusinessProfile().timezone
            if (tz.isBlank()) ZoneOffset.UTC else ZoneId.of(tz)
        } catch (e: Exception) {
            log.warn("Falling back to UTC for analytics bucketing: {}", e.message)
            ZoneOffset.UTC
        }
}
