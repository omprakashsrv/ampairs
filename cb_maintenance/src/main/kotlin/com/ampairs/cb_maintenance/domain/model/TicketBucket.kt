package com.ampairs.cb_maintenance.domain.model

import com.ampairs.cb_maintenance.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * One leaf of the maintenance ticket-classification taxonomy:
 * `department › category › subCategory1 [› subCategory2]`.
 *
 * Workspace-scoped reference data: extends [OwnableBaseDomain] (`@TenantId owner_id`), so each
 * workspace owns its own copy of the taxonomy and the `/sync` GET feed is tenant-filtered
 * automatically. Pull-only in the app — the device downloads its workspace's catalog to drive the
 * cascading Department → Category → Sub-category pickers on the raise-ticket form. The rows are
 * seeded per workspace (scoped to that workspace's `owner_id`), not by a global Flyway insert.
 * `subCategory2` is stored as `""` (not null) when the leaf has only three levels so the uniqueness
 * constraint behaves identically on PostgreSQL and MySQL.
 */
@Entity(name = "cb_ticket_bucket")
@NamedEntityGraph(name = "CbTicketBucket.basic")
@Table(
    name = "ticket_bucket",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_cb_ticket_bucket",
            columnNames = ["owner_id", "department", "category", "sub_category_1", "sub_category_2"],
        ),
    ],
    indexes = [
        Index(name = "idx_cb_ticket_bucket_owner", columnList = "owner_id"),
        Index(name = "idx_cb_ticket_bucket_dept", columnList = "department"),
    ]
)
class TicketBucket : OwnableBaseDomain() {

    @Column(name = "department", length = 100, nullable = false)
    var department: String = ""

    @Column(name = "category", length = 150, nullable = false)
    var category: String = ""

    @Column(name = "sub_category_1", length = 200, nullable = false)
    var subCategory1: String = ""

    @Column(name = "sub_category_2", length = 200, nullable = false)
    var subCategory2: String = ""

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return Constants.TICKET_BUCKET_PREFIX
    }
}
