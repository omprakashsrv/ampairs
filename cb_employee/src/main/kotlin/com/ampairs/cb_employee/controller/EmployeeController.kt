package com.ampairs.cb_employee.controller

import com.ampairs.cb_employee.domain.dto.EmployeeRequest
import com.ampairs.cb_employee.domain.dto.EmployeeResponse
import com.ampairs.cb_employee.exception.EmployeeNotFoundException
import com.ampairs.cb_employee.service.EmployeeService
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
@RequestMapping("/cb_employee/v1/employees")
@Validated
class EmployeeController(
    private val employeeService: EmployeeService,
) {

    /**
     * Incremental sync feed: employees updated at/after last_sync, INCLUDING inactive
     * (soft-deleted) rows, ordered by updatedAt ASC, paginated.
     */
    @GetMapping("/sync")
    fun getEmployeesSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<EmployeeResponse>> {
        val jpaPropertyName = when (sortBy) {
            "name" -> "name"
            "employeeNo" -> "employeeNo"
            "role" -> "role"
            "active" -> "active"
            "createdAt" -> "createdAt"
            "updatedAt" -> "updatedAt"
            else -> "updatedAt"
        }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaPropertyName))
        return ApiResponse.success(PageResponse.from(employeeService.getEmployeesAfterSync(lastSync, pageable)))
    }

    /**
     * Bulk upsert employees keyed by uid (create if absent, update if present). Soft-deleted rows
     * (active = false) are accepted in-band so deletions propagate without a per-row DELETE.
     */
    @PostMapping("/sync")
    fun bulkUpsertEmployees(
        @RequestBody requests: List<@Valid EmployeeRequest>,
    ): ApiResponse<List<EmployeeResponse>> {
        return ApiResponse.success(employeeService.bulkUpsert(requests))
    }

    @GetMapping("/{uid}")
    fun getEmployee(@PathVariable uid: String): ApiResponse<EmployeeResponse> {
        val employee = employeeService.findByUid(uid)
            ?: throw EmployeeNotFoundException("Employee not found for uid: $uid")
        return ApiResponse.success(employee)
    }
}
