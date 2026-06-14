package com.ampairs.form.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * Aggregate-root header for one entity-type's form schema, per workspace.
 *
 * The aggregate (this header + its [FormSection] and [FormField] rows, joined on
 * `owner_id` + `entity_type`) is the unit of sync: one record per entityType, transferred whole.
 * Per-workspace identity is enforced by `uk_form_schema_owner_entity` on
 * `(owner_id, entity_type)`; `uid` is the standard prefixed nanoid from [BaseDomain] (globally
 * unique system-wide), so clients key the aggregate on [entityType], not on `uid`. [version] is
 * the optimistic-concurrency stamp, bumped on every aggregate save.
 */
@Entity
@Table(
    name = "form_schema",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_form_schema_owner_entity", columnNames = ["owner_id", "entity_type"])
    ],
    indexes = [
        Index(name = "idx_form_schema_owner_entity", columnList = "owner_id,entity_type")
    ]
)
class FormSchema : OwnableBaseDomain() {

    @Column(name = "entity_type", nullable = false, length = 50)
    var entityType: String = ""

    @Column(name = "version", nullable = false)
    var version: Long = 0

    override fun obtainSeqIdPrefix(): String = "FORMSCHEMA"
}
