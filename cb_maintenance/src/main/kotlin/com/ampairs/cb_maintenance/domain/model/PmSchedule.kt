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

/**
 * A preventive-maintenance task attached to an asset category. Applies to EVERY store automatically
 * — no per-store row (module plan §3). Changing a cadence chain-wide is one row edit.
 */
@Entity(name = "cb_pm_schedule")
@NamedEntityGraph(name = "CbPmSchedule.basic")
@Table(
    name = "pm_schedule",
    indexes = [
        Index(name = "idx_cb_pm_schedule_uid", columnList = "uid", unique = true),
        Index(name = "idx_cb_pm_schedule_owner", columnList = "owner_id"),
        Index(name = "idx_cb_pm_schedule_category", columnList = "asset_category"),
    ]
)
class PmSchedule : OwnableBaseDomain() {

    @Column(name = "asset_category", length = 100, nullable = false)
    var assetCategory: String = ""

    @Column(name = "task_name", length = 200, nullable = false)
    var taskName: String = ""

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checklist", columnDefinition = "jsonb")
    var checklist: List<String>? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency_unit", length = 20, nullable = false)
    var frequencyUnit: FrequencyUnit = FrequencyUnit.MONTH

    /** MONTH + interval=3 == quarterly. */
    @Column(name = "frequency_interval", nullable = false)
    var frequencyInterval: Int = 1

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return Constants.PM_SCHEDULE_PREFIX
    }
}
