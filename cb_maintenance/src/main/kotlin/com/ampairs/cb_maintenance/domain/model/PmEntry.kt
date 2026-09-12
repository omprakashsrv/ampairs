package com.ampairs.cb_maintenance.domain.model

import com.ampairs.cb_maintenance.config.Constants
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
import java.time.Instant

/**
 * One occurrence of a PM task at a store — either SCHEDULED (rolled by the nightly job) or AD_HOC
 * (called in outside the cycle). One status workflow, one completion flow (module plan §3).
 */
@Entity(name = "cb_pm_entry")
@NamedEntityGraph(name = "CbPmEntry.basic")
@Table(
    name = "pm_entry",
    indexes = [
        Index(name = "idx_cb_pm_entry_uid", columnList = "uid", unique = true),
        Index(name = "idx_cb_pm_entry_owner", columnList = "owner_id"),
        Index(name = "idx_cb_pm_entry_scope", columnList = "zonal_office_id, store_id, status, due_date"),
        Index(name = "idx_cb_pm_entry_cursor", columnList = "store_id, pm_schedule_id, due_date"),
        Index(name = "idx_cb_pm_entry_assignee", columnList = "assigned_to_employee_id"),
    ]
)
class PmEntry : OwnableBaseDomain() {

    @Column(name = "store_id", length = 200, nullable = false)
    var storeId: String = ""

    /** Denormalized via StoreService.getZonalOfficeId() at creation — drives access scoping (§5). */
    @Column(name = "zonal_office_id", length = 200, nullable = false)
    var zonalOfficeId: String = ""

    @Column(name = "asset_category", length = 100, nullable = false)
    var assetCategory: String = ""

    /** Null only when truly ad hoc (no underlying schedule). */
    @Column(name = "pm_schedule_id", length = 200)
    var pmScheduleId: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20, nullable = false)
    var source: PmEntrySource = PmEntrySource.SCHEDULED

    @Column(name = "due_date", nullable = false)
    var dueDate: Instant = Instant.now()

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    var status: PmEntryStatus = PmEntryStatus.DUE

    /** The one accountable owner — drives escalation and load-balancing. */
    @Column(name = "assigned_to_employee_id", length = 200)
    var assignedToEmployeeId: String? = null

    /** Anyone else who pitched in; purely informational (§4.1). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "assisted_by_employee_ids", columnDefinition = "jsonb")
    var assistedByEmployeeIds: List<String>? = null

    @Column(name = "completed_at")
    var completedAt: Instant? = null

    /** Whoever actually closed it out — may or may not be the original assignee. */
    @Column(name = "completed_by_employee_id", length = 200)
    var completedByEmployeeId: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checklist_result", columnDefinition = "jsonb")
    var checklistResult: List<ChecklistItemResult>? = null

    /** Set when a checklist failure spawns a Ticket (§6). Kept for audit; the Ticket carries originPmEntryId. */
    @Column(name = "ticket_id", length = 200)
    var ticketId: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return Constants.PM_ENTRY_PREFIX
    }
}
