package com.ampairs.cb_employee.domain.model

import com.ampairs.cb_employee.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * Maintenance-org roster row. Models an **org-chart fact** (who reports to whom, for escalation),
 * deliberately orthogonal to workspace login/RBAC — a technician can exist here without ever
 * logging in (dispatched by phone/SMS). See the module plan §1.
 */
@Entity(name = "cb_employee")
@NamedEntityGraph(name = "CbEmployee.basic")
@Table(
    name = "employee",
    indexes = [
        Index(name = "idx_cb_employee_uid", columnList = "uid", unique = true),
        Index(name = "idx_cb_employee_owner", columnList = "owner_id"),
        Index(name = "idx_cb_employee_zone", columnList = "zonal_office_id"),
        Index(name = "idx_cb_employee_user", columnList = "user_id"),
    ]
)
class Employee : OwnableBaseDomain() {

    @Column(name = "employee_no", length = 50, nullable = false)
    var employeeNo: String = ""

    @Column(name = "name", length = 150, nullable = false)
    var name: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 40, nullable = false)
    var role: MaintenanceRole = MaintenanceRole.EXECUTIVE

    @Column(name = "email", length = 200)
    var email: String? = null

    @Column(name = "mobile", length = 30)
    var mobile: String? = null

    /** Self-referencing FK -> Employee.uid; null at the top of the maintenance org. */
    @Column(name = "reports_to_employee_id", length = 200)
    var reportsToEmployeeId: String? = null

    /** Opaque reference into cb_store.ZonalOffice — no enforced FK (see module plan §0). */
    @Column(name = "zonal_office_id", length = 200)
    var zonalOfficeId: String? = null

    /** Optional per-employee store override; narrows assignment/access before falling back to zone. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mapped_store_ids", columnDefinition = "jsonb")
    var mappedStoreIds: List<String>? = null

    /** Link to a logged-in ampairs user, only if this employee ever calls the API. */
    @Column(name = "user_id", length = 200)
    var userId: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return Constants.EMPLOYEE_PREFIX
    }
}
