package com.ampairs.connector.api

/** Result of applying one sparse row to a target entity. */
enum class WriteOutcome { CREATED, UPDATED, SKIPPED, FAILED }

/**
 * Cross-module SPI: each target module (customer, product, unit, ...) implements this so the
 * connector can apply mapped columns into that module's entities WITHOUT reaching into its
 * repositories directly (Constitution Principle IX — cross-module via public interfaces only).
 *
 * The connector resolves the right writer by [entityType] and calls [applySparse] with the columns
 * actually present in the row (already intersected with the installation's mapping allowlist).
 * Match is by `refId` (or `uid`) only; a non-matching row creates a new record. Columns NOT in
 * [presentColumns] MUST be left untouched (per-row presence-based partial update).
 */
interface ConnectorEntityWriter {
    /** The entity type this writer handles, e.g. "customer", "product". */
    val entityType: String

    /**
     * @param refId stable external id (Tally GUID) used to match an existing record; may be null.
     * @param uid client-authored uid alternative match key; may be null.
     * @param presentColumns Ampairs column name → value. A present key with null clears that column.
     * @return outcome + the columns actually applied + the resulting record uid.
     */
    fun applySparse(refId: String?, uid: String?, presentColumns: Map<String, Any?>): WriteResult
}

data class WriteResult(
    val outcome: WriteOutcome,
    val ampairsUid: String?,
    val appliedColumns: List<String>,
    val message: String? = null,
)
