package com.ampairs.workspace.printing.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.workspace.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table

/**
 * Workspace-scoped print template. Mirrors the mobile app's `PrintTemplateEntity` /
 * `TemplateDto` wire shape so the app's `/printing/v1/templates/sync` push/pull round-trips
 * cleanly (id = uid, document_type, printer_class, name, template_json, version, active).
 *
 * One template per (document_type, printer_class): e.g. THERMAL invoice vs PAGE invoice. The
 * `template_json` blob is an opaque, client-rendered layout — the backend treats it as storage
 * only and never interprets it.
 */
@Entity(name = "print_template")
@NamedEntityGraph(name = "PrintTemplate.basic")
@Table(
    indexes = [
        Index(name = "idx_print_template_uid", columnList = "uid", unique = true),
        Index(name = "idx_print_template_owner", columnList = "owner_id"),
        Index(name = "idx_print_template_doc_type", columnList = "document_type")
    ]
)
class PrintTemplate : OwnableBaseDomain() {

    /** Document the template renders: INVOICE, ORDER, RECEIPT, LABEL, … (matches app DocumentType). */
    @Column(name = "document_type", length = 50, nullable = false)
    var documentType: String = ""

    /** Printer family the template targets: THERMAL, PAGE, LABEL (matches app PrinterClass). */
    @Column(name = "printer_class", length = 30, nullable = false)
    var printerClass: String = ""

    @Column(name = "name", length = 150, nullable = false)
    var name: String = ""

    /** Opaque, client-rendered layout blob (JSON). Stored verbatim; never parsed server-side. */
    @Column(name = "template_json", columnDefinition = "TEXT", nullable = false)
    var templateJson: String = ""

    /** Client-managed revision counter (last-write-wins is by updatedAt, not this). */
    @Column(name = "template_version", nullable = false)
    var templateVersion: Long = 1

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    /** The chosen template for its (document_type, printer_class). Client-managed; one default per pair. */
    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false

    override fun obtainSeqIdPrefix(): String = Constants.PRINT_TEMPLATE_PREFIX
}
