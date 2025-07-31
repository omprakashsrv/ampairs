package com.ampairs.inventory.repository

import com.ampairs.inventory.domain.model.Inventory
import org.springframework.data.repository.CrudRepository

interface InventoryRepository : CrudRepository<Inventory, Long> {
    fun findBySeqId(seqId: String?): Inventory?
    fun findByRefId(refId: String?): Inventory?
    fun findByProductId(productId: String?): Inventory?

//    @Query("SELECT p FROM inventory p WHERE p.groupId IN (:ids)")
//    fun getInventory(ids: String): List<Inventory>
}