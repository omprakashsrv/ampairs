package com.ampairs.inventory.service

import com.ampairs.inventory.domain.model.Inventory
import com.ampairs.inventory.repository.InventoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull


@Service
class InventoryService(
    val inventoryRepository: InventoryRepository,
) {

    @Transactional
    fun updateInventories(inventories: List<Inventory>): List<Inventory> {
        inventories.forEach {
            if (it.id.isNotEmpty()) {
                val group = inventoryRepository.findById(it.id).getOrNull()
                it.seqId = group?.seqId
                it.refId = group?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val group = inventoryRepository.findByRefId(it.refId)
                it.seqId = group?.seqId
                it.id = group?.id ?: ""
            }
            inventoryRepository.save(it)
        }
        return inventories
    }
}