package com.ampairs.customer.connector

import com.ampairs.core.connector.ConnectorEntityWriter
import com.ampairs.core.connector.SparsePropertyApplier
import com.ampairs.core.connector.WriteOutcome
import com.ampairs.core.connector.WriteResult
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.repository.CustomerRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Applies connector-mapped columns onto Customer entities (spec 013, Principle IX). Matches an
 * existing customer by `refId` (e.g. Tally GUID) or `uid`; otherwise creates a new one. Only the
 * present, allowlisted columns are written — omitted columns are preserved.
 */
@Component
class ConnectorCustomerWriter(
    private val repository: CustomerRepository,
) : ConnectorEntityWriter {

    override val entityType: String = "customer"

    @Transactional
    override fun applySparse(refId: String?, uid: String?, presentColumns: Map<String, Any?>): WriteResult {
        val existing = refId?.takeIf { it.isNotBlank() }?.let { repository.findByRefId(it) }
            ?: uid?.takeIf { it.isNotBlank() }?.let { repository.findByUid(it) }
        val entity = existing ?: Customer().also { c ->
            uid?.takeIf { it.isNotBlank() }?.let { c.uid = it }
            refId?.takeIf { it.isNotBlank() }?.let { c.refId = it }
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
