package com.ampairs.cb_employee.service

import com.ampairs.cb_employee.domain.dto.EmployeeRequest
import com.ampairs.cb_employee.domain.dto.EmployeeResponse
import com.ampairs.cb_employee.domain.dto.applyRequest
import com.ampairs.cb_employee.domain.dto.asEmployeeResponse
import com.ampairs.cb_employee.domain.model.Employee
import com.ampairs.cb_employee.domain.model.MaintenanceRole
import com.ampairs.cb_employee.exception.EmployeeNotFoundException
import com.ampairs.cb_employee.repository.EmployeeRepository
import com.ampairs.core.sync.EntityChangePublisher
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

@Service
class EmployeeServiceImpl(
    private val employeeRepository: EmployeeRepository,
    private val entityChangePublisher: EntityChangePublisher,
) : EmployeeService {

    private val logger = LoggerFactory.getLogger(EmployeeServiceImpl::class.java)

    @Transactional(readOnly = true)
    override fun findByUid(uid: String): EmployeeResponse? {
        if (uid.isBlank()) return null
        return employeeRepository.findByUid(uid)?.asEmployeeResponse()
    }

    @Transactional(readOnly = true)
    override fun getByUid(uid: String): EmployeeResponse =
        findByUid(uid) ?: throw EmployeeNotFoundException("Employee not found for uid: $uid")

    @Transactional(readOnly = true)
    override fun getByUserId(userId: String): EmployeeResponse? {
        if (userId.isBlank()) return null
        return employeeRepository.findByUserId(userId)?.asEmployeeResponse()
    }

    @Transactional(readOnly = true)
    override fun findEligibleAssignees(zonalOfficeId: String): List<EmployeeResponse> {
        if (zonalOfficeId.isBlank()) return emptyList()
        return employeeRepository
            .findByZonalOfficeIdAndActiveTrueAndRoleNot(zonalOfficeId, MaintenanceRole.MAINTENANCE_LEADER)
            .map { it.asEmployeeResponse() }
    }

    @Transactional(readOnly = true)
    override fun findAllActive(): List<EmployeeResponse> =
        employeeRepository
            .findByActiveTrueAndRoleNot(MaintenanceRole.MAINTENANCE_LEADER)
            .map { it.asEmployeeResponse() }

    @Transactional(readOnly = true)
    override fun resolveEscalationTarget(employeeId: String): EmployeeResponse {
        var current = employeeRepository.findByUid(employeeId)
            ?: throw EmployeeNotFoundException("Employee not found for uid: $employeeId")
        val terminals = setOf(MaintenanceRole.ASSISTANT_MANAGER, MaintenanceRole.MAINTENANCE_LEADER)
        val visited = HashSet<String>()
        while (current.role !in terminals) {
            val next = current.reportsToEmployeeId?.takeIf { it.isNotBlank() } ?: break
            // Guard against a broken/cyclic chain — best-effort return the last resolvable row.
            if (!visited.add(next)) break
            current = employeeRepository.findByUid(next) ?: break
        }
        return current.asEmployeeResponse()
    }

    @Transactional(readOnly = true)
    override fun getEmployeesAfterSync(lastSync: String?, pageable: Pageable): Page<EmployeeResponse> {
        val page: Page<Employee> = if (lastSync.isNullOrBlank()) {
            employeeRepository.findAllForSync(pageable)
        } else {
            try {
                val decoded = URLDecoder.decode(lastSync, StandardCharsets.UTF_8)
                employeeRepository.findByUpdatedAtAfter(Instant.parse(decoded), pageable)
            } catch (e: Exception) {
                logger.warn("Invalid last_sync '{}', falling back to full sync feed", lastSync, e)
                employeeRepository.findAllForSync(pageable)
            }
        }
        return page.map { it.asEmployeeResponse() }
    }

    @Transactional
    override fun bulkUpsert(requests: List<EmployeeRequest>): List<EmployeeResponse> =
        requests.map { request ->
            val existing = request.uid?.takeIf { it.isNotBlank() }?.let { employeeRepository.findByUid(it) }
            if (existing != null) {
                existing.applyRequest(request)
                employeeRepository.save(existing)
                    .also { entityChangePublisher.updated("cb_employee", it.uid) }
                    .asEmployeeResponse()
            } else {
                val employee = Employee().applyRequest(request)
                employeeRepository.save(employee)
                    .also { entityChangePublisher.created("cb_employee", it.uid) }
                    .asEmployeeResponse()
            }
        }

    @Transactional
    override fun create(request: EmployeeRequest): EmployeeResponse {
        val employee = Employee().applyRequest(request)
        val saved = employeeRepository.save(employee)
        entityChangePublisher.created("cb_employee", saved.uid)
        return saved.asEmployeeResponse()
    }

    @Transactional
    override fun delete(uid: String) {
        val employee = employeeRepository.findByUid(uid)
            ?: throw EmployeeNotFoundException("Employee not found for uid: $uid")
        employee.active = false
        employeeRepository.save(employee)
        entityChangePublisher.deleted("cb_employee", employee.uid)
    }
}
