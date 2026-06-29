package com.ampairs.sfa.domain.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.sfa.domain.model.FieldOrder
import com.ampairs.sfa.repository.FieldOrderRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Counter-order pointers over `/sync`. The actual order rides the `order` module's own `/sync`;
 * this row only references the resulting `orderUid` for the DMS secondary-sales attribution layer.
 */
@Service
@Transactional
class FieldOrderService(
    private val fieldOrderRepository: FieldOrderRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {

    @Transactional(readOnly = true)
    fun getFieldOrdersAfterSync(lastSync: String?, pageable: Pageable): Page<FieldOrder> =
        syncFeed(lastSync, pageable, { fieldOrderRepository.findAll(it) }, { i, p -> fieldOrderRepository.findByUpdatedAtAfter(i, p) })

    fun bulkUpsertFieldOrders(incoming: List<FieldOrder>): List<FieldOrder> = incoming.map { row ->
        val existing = row.uid.takeIf { it.isNotBlank() }?.let { fieldOrderRepository.findByUid(it) }
        if (existing != null) {
            existing.visitUid = row.visitUid
            existing.customerUid = row.customerUid
            existing.repMemberUid = row.repMemberUid
            existing.orderUid = row.orderUid
            existing.amount = row.amount
            existing.active = row.active
            fieldOrderRepository.save(existing).also { entityChangePublisher.updated("field_order", it.uid) }
        } else {
            fieldOrderRepository.save(row).also { entityChangePublisher.created("field_order", it.uid) }
        }
    }
}
