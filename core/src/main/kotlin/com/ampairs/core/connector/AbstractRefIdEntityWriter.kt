package com.ampairs.core.connector

import com.ampairs.core.domain.model.OwnableBaseDomain

/**
 * Base [ConnectorEntityWriter] for tenant-scoped entities matched by `refId`/`uid`. Handles the common
 * flow: match existing → else create → apply present columns (presence-based partial update) → save.
 * Subclasses supply the per-module repository hooks. The enclosing connector sparse-upsert call is
 * already `@Transactional`, so individual saves participate in that transaction.
 *
 * Column application defaults to flat reflection ([SparsePropertyApplier]); override [applyColumns]
 * (or use [DslEntityWriter]) to map complex/nested targets.
 */
abstract class AbstractRefIdEntityWriter<T : OwnableBaseDomain> : ConnectorEntityWriter {

    protected abstract fun findByRefId(refId: String): T?
    protected abstract fun findByUid(uid: String): T?
    protected abstract fun newInstance(): T
    protected abstract fun persist(entity: T): T

    /** When false, a row that matches no existing record is SKIPPED instead of creating a new one. */
    protected open val createIfMissing: Boolean get() = true

    /** Apply the present columns onto the entity; returns the columns actually applied. */
    protected open fun applyColumns(entity: T, columns: Map<String, Any?>): List<String> =
        SparsePropertyApplier.apply(entity, columns)

    /** Hook for entity-specific defaults on a freshly created row (e.g. a required business code). */
    protected open fun onBeforeSave(entity: T, isNew: Boolean, presentColumns: Map<String, Any?>) {}

    override fun applySparse(refId: String?, uid: String?, presentColumns: Map<String, Any?>): WriteResult {
        val existing = refId?.takeIf { it.isNotBlank() }?.let { findByRefId(it) }
            ?: uid?.takeIf { it.isNotBlank() }?.let { findByUid(it) }

        if (existing == null && !createIfMissing) {
            return WriteResult(WriteOutcome.SKIPPED, null, emptyList(), "No matching $entityType for refId=$refId")
        }
        val entity = existing ?: newInstance().also { e ->
            uid?.takeIf { it.isNotBlank() }?.let { e.uid = it }
            refId?.takeIf { it.isNotBlank() }?.let { e.refId = it }
        }
        val applied = applyColumns(entity, presentColumns)
        onBeforeSave(entity, existing == null, presentColumns)
        val saved = persist(entity)
        return WriteResult(
            outcome = if (existing == null) WriteOutcome.CREATED else WriteOutcome.UPDATED,
            ampairsUid = saved.uid,
            appliedColumns = applied,
        )
    }
}
