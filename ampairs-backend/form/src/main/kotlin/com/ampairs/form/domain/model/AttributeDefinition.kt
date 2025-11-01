package com.ampairs.form.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * Generic attribute definition entity for any entity type
 * Defines custom attributes based on business vertical (retail, wholesale, etc.)
 */
@Entity
@Table(
    name = "attribute_definition",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["entity_type", "attribute_key"])
    ],
    indexes = [
        Index(name = "idx_owner_entity_type", columnList = "owner_id,entity_type")
    ]
)
class AttributeDefinition : OwnableBaseDomain() {

    @Column(name = "entity_type", nullable = false, length = 50)
    var entityType: String = ""

    @Column(name = "attribute_key", nullable = false, length = 100)
    var attributeKey: String = ""

    @Column(name = "display_name", nullable = false)
    var displayName: String = ""

    @Column(name = "data_type", nullable = false, length = 50)
    var dataType: String = "STRING"

    @Column(name = "visible", nullable = false)
    var visible: Boolean = true

    @Column(name = "mandatory", nullable = false)
    var mandatory: Boolean = false

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    @Column(name = "category", length = 100)
    var category: String? = null

    @Column(name = "default_value")
    var defaultValue: String? = null

    @Column(name = "validation_type", length = 50)
    var validationType: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_params")
    @JsonProperty("validation_params")
    var validationParams: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enum_values")
    @JsonProperty("enum_values")
    var enumValues: String? = null

    @Column(name = "placeholder")
    var placeholder: String? = null

    @Column(name = "help_text", columnDefinition = "TEXT")
    var helpText: String? = null

    override fun obtainSeqIdPrefix(): String {
        return "AD" // AttributeDefinition
    }
}
