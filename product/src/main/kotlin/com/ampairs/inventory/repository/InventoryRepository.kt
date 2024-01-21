package com.ampairs.inventory.repository

import com.ampairs.inventory.domain.model.Inventory
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface InventoryRepository : CrudRepository<Inventory, String> {
    fun findByRefId(refId: String?): Inventory?
    fun findByProductId(productId: String?): Inventory?

    @Query("SELECT pd FROM inventory pd WHERE pd.id = :id")
    override fun findById(id: String): Optional<Inventory>

//    @Query("SELECT p FROM inventory p WHERE p.groupId IN (:ids)")
//    fun getInventory(ids: String): List<Inventory>
}