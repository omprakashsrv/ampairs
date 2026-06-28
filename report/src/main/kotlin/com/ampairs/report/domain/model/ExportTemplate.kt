package com.ampairs.report.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.report.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table

/**
 * A saved, syncable, per-module custom report ("Export Template"): which columns to export,
 * what filters/sort to apply, and the default format / generation location. Workspace-scoped
 * and exposed on the canonical `/sync` contract so a report configured on one device is
 * available (offline) on every device.
 *
 * `selectedColumns` and `filters` are stored as opaque JSON text (serialized by the service)
 * to keep the persistence layer free of structured query knowledge.
 */
@Entity(name = "export_template")
@NamedEntityGraph(name = "ExportTemplate.basic")
@Table(
    indexes = [
        Index(name = "idx_export_template_uid", columnList = "uid", unique = true),
        Index(name = "idx_export_template_owner", columnList = "owner_id"),
        Index(name = "idx_export_template_module", columnList = "module_key"),
    ]
)
class ExportTemplate : OwnableBaseDomain() {

    @Column(name = "module_key", length = 100, nullable = false)
    var moduleKey: String = ""

    @Column(name = "name", length = 200, nullable = false)
    var name: String = ""

    /** JSON array of column keys (ordered). Empty/null ⇒ standard report (all columns). */
    @Column(name = "selected_columns", columnDefinition = "TEXT")
    var selectedColumns: String? = null

    /** JSON array of `{column_key, op, value}` filter objects. */
    @Column(name = "filters", columnDefinition = "TEXT")
    var filters: String? = null

    @Column(name = "sort_by", length = 100)
    var sortBy: String? = null

    @Column(name = "sort_dir", length = 4, nullable = false)
    var sortDir: String = "ASC"

    @Column(name = "default_format", length = 10, nullable = false)
    var defaultFormat: String = "CSV"

    @Column(name = "default_location", length = 10, nullable = false)
    var defaultLocation: String = "CLIENT"

    @Column(name = "include_inactive", nullable = false)
    var includeInactive: Boolean = false

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.EXPORT_TEMPLATE_PREFIX
}
