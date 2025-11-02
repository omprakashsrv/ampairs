package com.ampairs.core.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Utility functions for timezone operations.
 *
 * Provides helper methods for working with Instant (UTC) timestamps
 * and converting between timezones when needed.
 *
 * **Design Principle**: Always store and work with Instant (UTC).
 * Only convert to LocalDateTime when absolutely necessary (e.g., display).
 *
 * @see java.time.Instant
 * @see com.ampairs.core.domain.model.BaseDomain
 */
object TimeUtils {

    /**
     * Get current time as Instant (UTC).
     *
     * Equivalent to Instant.now() but provides a consistent API.
     *
     * @return Current moment in UTC
     */
    fun now(): Instant = Instant.now()

    /**
     * Convert Instant to LocalDateTime in given timezone.
     *
     * **Use sparingly** - only for display or when timezone context is explicit.
     * Most operations should work directly with Instant.
     *
     * @param instant The Instant to convert
     * @param zoneId The target timezone (default: UTC)
     * @return LocalDateTime in the specified timezone
     */
    fun toLocalDateTime(instant: Instant, zoneId: ZoneId = ZoneId.of("UTC")): LocalDateTime {
        return LocalDateTime.ofInstant(instant, zoneId)
    }

    /**
     * Convert LocalDateTime (assumed UTC) to Instant.
     *
     * **Important**: This assumes the LocalDateTime is in UTC timezone.
     * If the LocalDateTime is in a different timezone, use toInstantWithZone() instead.
     *
     * @param localDateTime LocalDateTime assumed to be in UTC
     * @return Instant representing that moment
     */
    fun toInstant(localDateTime: LocalDateTime): Instant {
        return localDateTime.toInstant(ZoneOffset.UTC)
    }

    /**
     * Convert LocalDateTime in specified timezone to Instant.
     *
     * Use this when the LocalDateTime is known to be in a specific timezone.
     *
     * @param localDateTime The LocalDateTime to convert
     * @param zoneId The timezone the LocalDateTime is in
     * @return Instant representing that moment in UTC
     */
    fun toInstantWithZone(localDateTime: LocalDateTime, zoneId: ZoneId): Instant {
        return localDateTime.atZone(zoneId).toInstant()
    }

    /**
     * Format Instant as ISO-8601 string with Z suffix.
     *
     * Output format: "2025-01-09T14:30:00Z" or "2025-01-09T14:30:00.123Z"
     *
     * This is the format used in API responses.
     *
     * @param instant The Instant to format
     * @return ISO-8601 string with Z suffix
     */
    fun formatIso8601(instant: Instant): String {
        return instant.toString()
    }

    /**
     * Parse ISO-8601 string to Instant.
     *
     * Accepts formats:
     * - "2025-01-09T14:30:00Z"
     * - "2025-01-09T14:30:00.123Z"
     * - "2025-01-09T14:30:00+00:00"
     *
     * @param isoString ISO-8601 formatted string
     * @return Instant parsed from string
     * @throws java.time.format.DateTimeParseException if format is invalid
     */
    fun parseIso8601(isoString: String): Instant {
        return Instant.parse(isoString)
    }

    /**
     * Format Instant for human-readable display in specified timezone.
     *
     * Example output: "2025-01-09 14:30:00"
     *
     * @param instant The Instant to format
     * @param zoneId Timezone for display (default: UTC)
     * @param pattern DateTimeFormatter pattern (default: "yyyy-MM-dd HH:mm:ss")
     * @return Formatted string in specified timezone
     */
    fun formatForDisplay(
        instant: Instant,
        zoneId: ZoneId = ZoneId.of("UTC"),
        pattern: String = "yyyy-MM-dd HH:mm:ss"
    ): String {
        val formatter = DateTimeFormatter.ofPattern(pattern).withZone(zoneId)
        return formatter.format(instant)
    }

    /**
     * Get Instant from epoch milliseconds.
     *
     * Useful for converting from Unix timestamps or System.currentTimeMillis().
     *
     * @param epochMilli Milliseconds since Unix epoch (1970-01-01T00:00:00Z)
     * @return Instant representing that moment
     */
    fun fromEpochMilli(epochMilli: Long): Instant {
        return Instant.ofEpochMilli(epochMilli)
    }

    /**
     * Get Instant from epoch seconds.
     *
     * Useful for converting from Unix timestamps in seconds.
     *
     * @param epochSecond Seconds since Unix epoch (1970-01-01T00:00:00Z)
     * @return Instant representing that moment
     */
    fun fromEpochSecond(epochSecond: Long): Instant {
        return Instant.ofEpochSecond(epochSecond)
    }

    /**
     * Check if an Instant is in the past.
     *
     * @param instant The Instant to check
     * @return true if instant is before current time
     */
    fun isInPast(instant: Instant): Boolean {
        return instant.isBefore(Instant.now())
    }

    /**
     * Check if an Instant is in the future.
     *
     * @param instant The Instant to check
     * @return true if instant is after current time
     */
    fun isInFuture(instant: Instant): Boolean {
        return instant.isAfter(Instant.now())
    }

    /**
     * Get age of an Instant in seconds.
     *
     * Useful for checking how old a timestamp is.
     *
     * @param instant The Instant to check
     * @return Number of seconds since the instant (negative if in future)
     */
    fun ageInSeconds(instant: Instant): Long {
        return Instant.now().epochSecond - instant.epochSecond
    }

    /**
     * Get age of an Instant in days.
     *
     * @param instant The Instant to check
     * @return Number of days since the instant (negative if in future)
     */
    fun ageInDays(instant: Instant): Long {
        return ageInSeconds(instant) / 86400 // 24 * 60 * 60
    }

    /**
     * Common timezone constants for convenience.
     */
    object Zones {
        val UTC: ZoneId = ZoneId.of("UTC")
        val IST: ZoneId = ZoneId.of("Asia/Kolkata")
        val EST: ZoneId = ZoneId.of("America/New_York")
        val PST: ZoneId = ZoneId.of("America/Los_Angeles")
        val GMT: ZoneId = ZoneId.of("GMT")
    }
}
