package com.ampairs.product.connector

import com.ampairs.core.connector.ConnectorEntityWriter
import com.ampairs.core.connector.SparsePropertyApplier
import com.ampairs.core.connector.WriteOutcome
import com.ampairs.core.connector.WriteResult
import com.ampairs.product.domain.model.Product
import com.ampairs.product.repository.ProductRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Applies connector-mapped columns onto Product entities (spec 013, Principle IX). Matches by `refId`
 * (e.g. Tally GUID) or `uid`; otherwise creates a new product. Only present, allowlisted columns are
 * written — omitted columns are preserved.
 */
@Component
class ConnectorProductWriter(
    private val repository: ProductRepository,
) : ConnectorEntityWriter {

    override val entityType: String = "product"

    @Transactional
    override fun applySparse(refId: String?, uid: String?, presentColumns: Map<String, Any?>): WriteResult {
        val existing = refId?.takeIf { it.isNotBlank() }?.let { repository.findByRefId(it) }
            ?: uid?.takeIf { it.isNotBlank() }?.let { repository.findByUid(it) }
        val entity = existing ?: Product().also { p ->
            uid?.takeIf { it.isNotBlank() }?.let { p.uid = it }
            refId?.takeIf { it.isNotBlank() }?.let { p.refId = it }
        }
        val applied = SparsePropertyApplier.apply(entity, presentColumns)
        val saved = repository.save(entity)
        return WriteResult(
            outcome = if (existing == null) WriteOutcome.CREATED else WriteOutcome.UPDATED,
            ampairsUid = saved.uid,
            appliedColumns = applied,
        )
    }
}
