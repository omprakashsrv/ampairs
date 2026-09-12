package com.ampairs.cb_employee.domain.dto

import com.ampairs.cb_employee.domain.model.Employee
import com.ampairs.cb_employee.domain.model.MaintenanceRole
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class EmployeeRequest(
    val uid: String? = null,

    @field:NotBlank(message = "Employee number is required")
    @field:Size(max = 50, message = "Employee number must not exceed 50 characters")
    val employeeNo: String,

    @field:NotBlank(message = "Employee name is required")
    @field:Size(max = 150, message = "Employee name must not exceed 150 characters")
    val name: String,

    val role: MaintenanceRole = MaintenanceRole.EXECUTIVE,

    @field:Size(max = 200, message = "Email must not exceed 200 characters")
    val email: String? = null,

    @field:Size(max = 30, message = "Mobile must not exceed 30 characters")
    val mobile: String? = null,

    val reportsToEmployeeId: String? = null,

    val zonalOfficeId: String? = null,

    val mappedStoreIds: List<String>? = null,

    val userId: String? = null,

    val active: Boolean = true,

    val refId: String? = null,
)

data class EmployeeResponse(
    val uid: String,
    val refId: String?,
    val employeeNo: String,
    val name: String,
    val role: MaintenanceRole,
    val email: String?,
    val mobile: String?,
    val reportsToEmployeeId: String?,
    val zonalOfficeId: String?,
    val mappedStoreIds: List<String>?,
    val userId: String?,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun Employee.applyRequest(request: EmployeeRequest): Employee = apply {
    request.uid?.let { uid = it }
    employeeNo = request.employeeNo.trim()
    name = request.name.trim()
    role = request.role
    email = request.email?.trim()
    mobile = request.mobile?.trim()
    reportsToEmployeeId = request.reportsToEmployeeId?.takeIf { it.isNotBlank() }
    zonalOfficeId = request.zonalOfficeId?.takeIf { it.isNotBlank() }
    mappedStoreIds = request.mappedStoreIds?.takeIf { it.isNotEmpty() }
    userId = request.userId?.takeIf { it.isNotBlank() }
    active = request.active
    request.refId?.takeIf { it.isNotBlank() }?.let { refId = it }
}

fun Employee.asEmployeeResponse(): EmployeeResponse = EmployeeResponse(
    uid = uid,
    refId = refId,
    employeeNo = employeeNo,
    name = name,
    role = role,
    email = email,
    mobile = mobile,
    reportsToEmployeeId = reportsToEmployeeId,
    zonalOfficeId = zonalOfficeId,
    mappedStoreIds = mappedStoreIds,
    userId = userId,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<Employee>.asEmployeeResponses(): List<EmployeeResponse> = map { it.asEmployeeResponse() }
