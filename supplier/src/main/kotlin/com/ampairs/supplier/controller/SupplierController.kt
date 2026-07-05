package com.ampairs.supplier.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.exception.NotFoundException
import com.ampairs.supplier.domain.dto.*
import com.ampairs.supplier.domain.service.SupplierService
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/supplier/v1")
@Tag(name = "Supplier Management", description = "Supplier CRUD and management operations")
class SupplierController(
    private val supplierService: SupplierService,
) {

    /**
     * Bulk upsert (push) endpoint of the unified sync contract. The body MAY contain
     * soft-deleted rows (status = DELETED) so deletions propagate in-band — there is no
     * separate per-row DELETE call in the sync push path.
     */
    @PostMapping("/suppliers/sync")
    fun updateSuppliers(@RequestBody supplierUpdateRequest: List<@Valid SupplierUpdateRequest>): ApiResponse<List<SupplierResponse>> {
        val suppliers = supplierUpdateRequest.toSuppliers()
        val result = supplierService.updateSuppliers(suppliers).asSuppliersResponse()
        return ApiResponse.success(result)
    }

    /**
     * Incremental sync feed (pull): suppliers updated at/after last_sync, INCLUDING
     * soft-deleted rows (status = DELETED) so clients can detect and propagate deletions.
     */
    @GetMapping("/suppliers/sync")
    fun getSuppliers(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String
    ): ApiResponse<PageResponse<SupplierResponse>> {
        val jpaPropertyName = when (sortBy) {
            "createdAt" -> "createdAt"
            "updatedAt" -> "updatedAt"
            "name" -> "name"
            "supplierType" -> "supplierType"
            "phone" -> "phone"
            "email" -> "email"
            else -> "updatedAt"
        }

        val sort = Sort.by(Sort.Direction.fromString(sortDir), jpaPropertyName)
        val pageable = PageRequest.of(page, size, sort)

        val suppliersPage = supplierService.getSuppliersAfterSync(lastSync, pageable)
        return ApiResponse.success(PageResponse.from(suppliersPage) { it.asSupplierResponse() })
    }

    @GetMapping("/{supplierId}")
    fun getSupplier(@PathVariable supplierId: String): ApiResponse<SupplierResponse> {
        val supplier = supplierService.getSupplierByUid(supplierId)
            ?: throw NotFoundException("Supplier not found: $supplierId")
        return ApiResponse.success(supplier.asSupplierResponse())
    }
}
