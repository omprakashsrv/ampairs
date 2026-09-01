package com.ampairs.core.connector

/** Result of applying one sparse row to a target entity. */
enum class WriteOutcome { CREATED, UPDATED, SKIPPED, FAILED }

data class WriteResult(
    val outcome: WriteOutcome,
    val ampairsUid: String?,
    val appliedColumns: List<String>,
    val message: String? = null,
)

/**
 * Cross-module SPI for the Apps & Extensions connector platform (spec 013). Each target module
 * (customer, product, unit, ...) provides a bean implementing this so the `connector` module can
 * apply mapped columns into that module's entities WITHOUT reaching into its repositories directly
 * (Constitution Principle IX — cross-module via public interfaces). Lives in `core` so target modules
 * implement it without depending on the `connector` module.
 *
 * The connector resolves the writer by [entityType] and calls [applySparse] with the columns actually
 * present in the row (already intersected with the installation's mapping allowlist). Match is by
 * `refId` (or `uid`) only; a non-matching row creates a new record. Columns NOT in [presentColumns]
 * MUST be left untouched (per-row presence-based partial update).
 */
interface ConnectorEntityWriter {
    /** The entity type this writer handles, e.g. "customer", "product". */
    val entityType: String

    /**
     * @param refId stable external id (e.g. Tally GUID) used to match an existing record; may be null.
     * @param uid client-authored uid alternative match key; may be null.
     * @param presentColumns Ampairs column/property name → value. A present key with null clears it.
     */
    fun applySparse(refId: String?, uid: String?, presentColumns: Map<String, Any?>): WriteResult
}
