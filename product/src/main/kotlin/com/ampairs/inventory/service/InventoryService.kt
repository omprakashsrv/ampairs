package com.ampairs.inventory.service

import com.ampairs.inventory.domain.dto.InventoryRequest
import com.ampairs.inventory.domain.dto.asDatabaseModel
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
    fun updateInventories(inventoryRequests: List<InventoryRequest>): List<Inventory> {
        var inventories = mutableListOf<Inventory>()
        inventoryRequests.forEach {
            val inventory = it.asDatabaseModel()
            if (it.id?.isNotEmpty() == true) {
                val group = inventoryRepository.findById(it.id).getOrNull()
                inventory.seqId = group?.seqId.toString()
                inventory.refId = group?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val group = inventoryRepository.findByRefId(it.refId)
                inventory.seqId = group?.seqId.toString()
                inventory.id = group?.id ?: ""
            } else if (it.productId?.isNotEmpty() == true) {
                val group = inventoryRepository.findByProductId(it.productId)
                inventory.seqId = group?.seqId.toString()
                inventory.id = group?.id ?: ""
            }
            inventories.add(inventoryRepository.save(inventory))
        }
        return inventories
    }
}