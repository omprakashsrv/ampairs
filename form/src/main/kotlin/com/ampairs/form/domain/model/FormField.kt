package com.ampairs.form.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * The single unified field model (replaces FieldConfig + AttributeDefinition). A member of the
 * [FormSchema] aggregate. Belongs to exactly one [FormSection] via [sectionUid] (mandatory).
 * No soft-delete column — removal is by absence on the next aggregate save.
 *
 * `enumValues` and `validationRules` are stored as JSON strings (de/serialized in the DTO mappers).
 */
@Entity
@Table(
    name = "form_field",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_form_field_owner_entity_source_key", columnNames = ["owner_id", "entity_type", "source", "field_key"])
    ],
    indexes = [
        Index(name = "idx_form_field_owner_entity", columnList = "owner_id,entity_type"),
        Index(name = "idx_form_field_section_order", columnList = "entity_type,section_uid,display_order")
    ]
)
class FormField : OwnableBaseDomain() {

    @Column(name = "entity_type", nullable = false, length = 50)
    var entityType: String = ""

    @Column(name = "source", nullable = false, length = 20)
    var source: String = FieldSource.CUSTOM.value

    @Column(name = "field_key", nullable = false, length = 100)
    var fieldKey: String = ""

    @Column(name = "display_name", nullable = false)
    var displayName: String = ""

    @Column(name = "data_type", nullable = false, length = 20)
    var dataType: String = FieldDataType.TEXT.value

    @Column(name = "widget_key", length = 100)
    var widgetKey: String? = null

    @Column(name = "section_uid", nullable = false, length = 200)
    var sectionUid: String = ""

    @Column(name = "visible", nullable = false)
    var visible: Boolean = true

    @Column(name = "mandatory", nullable = false)
    var mandatory: Boolean = false

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    @Column(name = "default_value")
    var defaultValue: String? = null

    @Column(name = "option_source", length = 20)
    var optionSource: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enum_values")
    var enumValues: String? = null

    @Column(name = "dynamic_source_key", length = 100)
    var dynamicSourceKey: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_rules")
    var validationRules: String? = null

    @Column(name = "placeholder")
    var placeholder: String? = null

    @Column(name = "help_text", columnDefinition = "TEXT")
    var helpText: String? = null

    override fun obtainSeqIdPrefix(): String = "FF"
}
