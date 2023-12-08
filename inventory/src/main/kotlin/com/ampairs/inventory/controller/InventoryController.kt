package com.ampairs.inventory.controller

import com.ampairs.inventory.domain.dto.InventoryRequest
import com.ampairs.inventory.domain.dto.InventoryResponse
import com.ampairs.inventory.domain.dto.asDatabaseModel
import com.ampairs.inventory.domain.dto.asResponse
import com.ampairs.inventory.service.InventoryService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/inventory/v1")
class InventoryController @Autowired constructor(
    private val inventoryService: InventoryService,
) {

    @GetMapping("")
    fun getInventories(@RequestParam("last_updated") lastUpdated: Long?) {

    }

    @PostMapping("/inventories")
    fun updateInventories(@RequestBody @Valid inventoryUpdateRequest: List<InventoryRequest>): List<InventoryResponse> {
        val inventories = inventoryUpdateRequest.asDatabaseModel()
        return inventoryService.updateInventories(inventories).asResponse()
    }

}