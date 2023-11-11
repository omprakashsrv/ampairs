package com.ampairs.inventory.controller

import com.ampairs.inventory.service.InventoryService
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

    @PostMapping("")
    fun updateInventories(@RequestParam("last_updated") lastUpdated: Long?) {

    }

}