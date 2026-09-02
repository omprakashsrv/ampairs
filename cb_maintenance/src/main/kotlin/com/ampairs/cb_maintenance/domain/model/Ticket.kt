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
 * Reactive maintenance ticket ("AC not cooling", "chest freezer gasket broken"), or a ticket spawned
 * by a failed PM check (see [originPmEntryId]). Shares the equipment taxonomy with [PmSchedule]
 * (module plan §3.1).
 */
@Entity(name = "cb_ticket")
@NamedEntityGraph(name = "CbTicket.basic")
@Table(
    name = "ticket",
    indexes = [
        Index(name = "idx_cb_ticket_uid", columnList = "uid", unique = true),
        Index(name = "idx_cb_ticket_owner", columnList = "owner_id"),
        Index(name = "idx_cb_ticket_scope", columnList = "zonal_office_id, store_id, status, raised_at"),
        Index(name = "idx_cb_ticket_assignee", columnList = "assigned_to_employee_id"),
    ]
)
class Ticket : OwnableBaseDomain() {

    @Column(name = "store_id", length = 200, nullable = false)
    var storeId: String = ""

    /** Denormalized via StoreService.getZonalOfficeId() at creation — drives access scoping (§5). */
    @Column(name = "zonal_office_id", length = 200, nullable = false)
    var zonalOfficeId: String = ""

    @Column(name = "asset_category", length = 100, nullable = false)
    var assetCategory: String = ""

    @Column(name = "sub_category", length = 150, nullable = false)
    var subCategory: String = ""

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    var status: TicketStatus = TicketStatus.OPEN

    /** The one accountable owner — drives escalation and load-balancing (§4.1). */
    @Column(name = "assigned_to_employee_id", length = 200)
    var assignedToEmployeeId: String? = null

    /** Anyone else who pitched in; purely informational (§4.1). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "assisted_by_employee_ids", columnDefinition = "jsonb")
    var assistedByEmployeeIds: List<String>? = null

    @Column(name = "raised_by_employee_id", length = 200)
    var raisedByEmployeeId: String? = null

    @Column(name = "raised_at", nullable = false)
    var raisedAt: Instant = Instant.now()

    @Column(name = "resolved_at")
    var resolvedAt: Instant? = null

    /** Set when a failed PM check spawned this ticket (§6). */
    @Column(name = "origin_pm_entry_id", length = 200)
    var originPmEntryId: String? = null

    /** Free text for now — no product-module linkage yet (§7). */
    @Column(name = "suggested_spare_part", length = 300)
    var suggestedSparePart: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return Constants.TICKET_PREFIX
    }
}
