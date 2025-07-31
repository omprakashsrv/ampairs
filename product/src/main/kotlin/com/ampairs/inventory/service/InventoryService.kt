package com.ampairs.inventory.service

import com.ampairs.inventory.domain.dto.InventoryRequest
import com.ampairs.inventory.domain.dto.asDatabaseModel
import com.ampairs.inventory.domain.model.Inventory
import com.ampairs.inventory.repository.InventoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class InventoryService(
    val inventoryRepository: InventoryRepository,
) {

    @Transactional
    fun updateInventories(inventoryRequests: List<InventoryRequest>): List<Inventory> {
        var inventories = mutableListOf<Inventory>()
        inventoryRequests.forEach {
            val inventory = it.asDatabaseModel()
            if (it.id?.isNotEmpty() == true) {
                val group = inventoryRepository.findBySeqId(it.id)
                inventory.id = group?.id ?: 0
                inventory.refId = group?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val group = inventoryRepository.findByRefId(it.refId)
                inventory.id = group?.id ?: 0
                inventory.seqId = group?.seqId ?: ""
            } else if (it.productId?.isNotEmpty() == true) {
                val group = inventoryRepository.findByProductId(it.productId)
                inventory.id = group?.id ?: 0
                inventory.seqId = group?.seqId ?: ""
            }
            inventories.add(inventoryRepository.save(inventory))
        }
        return inventories
    }
}