package com.ampairs.sequence.service

/**
 * Single source of truth for sequence value math and formatting. The mobile client
 * mirrors this logic for offline formatting from allocation snapshots — keep both in sync.
 */
object SequenceFormatter {

    private val DEFAULT_PREFIXES = mapOf(
        "product" to "PRD",
        "customer" to "CUS",
        "order" to "ORD",
        "invoice" to "INV",
    )

    /**
     * Next value to issue. `currentValue` is the last issued value (0 = nothing issued).
     * Raising `startValue` above the high-water mark moves issuance forward; it can
     * never re-issue already-used values.
     */
    fun nextValue(currentValue: Long, startValue: Long, incrementStep: Int): Long =
        if (currentValue < startValue) startValue else currentValue + incrementStep

    /** `[prefix-]paddedValue[-suffix]` — separator omitted when prefix/suffix absent. */
    fun format(prefix: String?, suffix: String?, paddingLength: Int, value: Long): String = buildString {
        prefix?.takeIf { it.isNotBlank() }?.let {
            append(it)
            append('-')
        }
        append(value.toString().padStart(paddingLength, '0'))
        suffix?.takeIf { it.isNotBlank() }?.let {
            append('-')
            append(it)
        }
    }

    /** Standard prefix for auto-provisioned default definitions. */
    fun defaultPrefix(entityType: String): String =
        DEFAULT_PREFIXES[entityType.lowercase()]
            ?: entityType.filter { it.isLetterOrDigit() }.take(3).uppercase().ifBlank { "SEQ" }
}
