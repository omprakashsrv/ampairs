package com.ampairs.cb_store.controller

import com.ampairs.cb_store.domain.dto.StoreRequest
import com.ampairs.cb_store.domain.dto.StoreResponse
import com.ampairs.cb_store.exception.StoreNotFoundException
import com.ampairs.cb_store.service.StoreService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cb_store/v1/stores")
@Validated
class StoreController(
    private val storeService: StoreService,
) {

    @GetMapping("/sync")
    fun getStoresSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<StoreResponse>> {
        val jpaPropertyName = when (sortBy) {
            "code" -> "code"
            "name" -> "name"
            "city" -> "city"
            "active" -> "active"
            "createdAt" -> "createdAt"
            "updatedAt" -> "updatedAt"
            else -> "updatedAt"
        }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaPropertyName))
        return ApiResponse.success(PageResponse.from(storeService.getStoresAfterSync(lastSync, pageable)))
    }

    @PostMapping("/sync")
    fun bulkUpsertStores(
        @RequestBody requests: List<@Valid StoreRequest>,
    ): ApiResponse<List<StoreResponse>> {
        return ApiResponse.success(storeService.bulkUpsert(requests))
    }

    @GetMapping("/{uid}")
    fun getStore(@PathVariable uid: String): ApiResponse<StoreResponse> {
        val store = storeService.findByUid(uid)
            ?: throw StoreNotFoundException("Store not found for uid: $uid")
        return ApiResponse.success(store)
    }
}
