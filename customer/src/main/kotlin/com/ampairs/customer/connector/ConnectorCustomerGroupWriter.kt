package com.ampairs.customer.connector

import com.ampairs.core.connector.AbstractRefIdEntityWriter
import com.ampairs.customer.domain.model.CustomerGroup
import com.ampairs.customer.repository.CustomerGroupRepository
import org.springframework.stereotype.Component

/**
 * Applies connector-mapped columns onto CustomerGroup entities (spec 013, Principle IX). Matched by
 * `refId` (Tally group name) or `uid`. CustomerGroup requires a unique `groupCode` per workspace, so
 * a new group gets one derived from its name/refId when the mapping doesn't supply one.
 */
@Component
class ConnectorCustomerGroupWriter(
    private val repository: CustomerGroupRepository,
) : AbstractRefIdEntityWriter<CustomerGroup>() {
    override val entityType: String = "customer_group"
    override fun findByRefId(refId: String): CustomerGroup? = repository.findByRefId(refId)
    override fun findByUid(uid: String): CustomerGroup? = repository.findByUid(uid)
    override fun newInstance(): CustomerGroup = CustomerGroup()
    override fun persist(entity: CustomerGroup): CustomerGroup = repository.save(entity)

    override fun onBeforeSave(entity: CustomerGroup, isNew: Boolean, presentColumns: Map<String, Any?>) {
        if (entity.groupCode.isBlank()) {
            val basis = entity.name.ifBlank { entity.refId ?: entity.uid }
            entity.groupCode = basis.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_').take(20)
                .ifBlank { "GRP_" + (entity.refId ?: "").take(16) }
        }
    }
}
