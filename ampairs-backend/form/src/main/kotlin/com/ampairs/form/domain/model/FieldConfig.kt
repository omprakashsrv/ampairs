package com.ampairs.form.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*

/**
 * Generic field configuration entity for any entity type
 * Controls visibility, validation, and behavior of standard entity fields
 */
@Entity
@Table(
    name = "field_config",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["entity_type", "field_name"])
    ],
    indexes = [
        Index(name = "idx_owner_entity_type", columnList = "owner_id,entity_type")
    ]
)
class FieldConfig : OwnableBaseDomain() {

    @Column(name = "entity_type", nullable = false, length = 50)
    var entityType: String = ""

    @Column(name = "field_name", nullable = false, length = 100)
    var fieldName: String = ""

    @Column(name = "display_name", nullable = false)
    var displayName: String = ""

    @Column(name = "visible", nullable = false)
    var visible: Boolean = true

    @Column(name = "mandatory", nullable = false)
    var mandatory: Boolean = false

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    @Column(name = "validation_type", length = 50)
    var validationType: String? = null

    @Column(name = "validation_params", columnDefinition = "JSON")
    @JsonProperty("validation_params")
    var validationParams: String? = null

    @Column(name = "placeholder")
    var placeholder: String? = null

    @Column(name = "help_text", columnDefinition = "TEXT")
    var helpText: String? = null

    @Column(name = "default_value")
    var defaultValue: String? = null

    override fun obtainSeqIdPrefix(): String {
        return "FC" // FieldConfig
    }
}
