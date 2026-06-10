package com.ampairs.form.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * A first-class, configurable section grouping fields within one entity-type form (a member of the
 * [FormSchema] aggregate). No soft-delete column — removal is by absence on the next aggregate save.
 */
@Entity
@Table(
    name = "form_section",
    indexes = [
        Index(name = "idx_form_section_owner_entity_order", columnList = "owner_id,entity_type,display_order")
    ]
)
class FormSection : OwnableBaseDomain() {

    @Column(name = "entity_type", nullable = false, length = 50)
    var entityType: String = ""

    @Column(name = "name", nullable = false)
    var name: String = ""

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    @Column(name = "visible", nullable = false)
    var visible: Boolean = true

    override fun obtainSeqIdPrefix(): String = "FS"
}
