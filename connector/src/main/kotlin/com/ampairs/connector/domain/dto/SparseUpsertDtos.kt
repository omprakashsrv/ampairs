package com.ampairs.connector.domain.dto

/**
 * ONE external record for the connector sparse upsert. [values] is a SPARSE MAP keyed by Ampairs
 * column/property name — KEY PRESENCE is the signal: a column absent from [values] is left untouched,
 * a column present with null intentionally clears it. The server intersects keys with the
 * installation's mapping allowlist and ignores keys outside it. (FR-018/FR-018b/FR-018c)
 */
data class SparseUpsertRow(
    val refId: String? = null,
    val uid: String? = null,
    val values: Map<String, Any?> = emptyMap(),
)

data class SparseUpsertResult(
    val refId: String?,
    val ampairsUid: String?,
    val outcome: String,
    val appliedColumns: List<String>,
    val message: String? = null,
)
