package com.ampairs.communication.domain.model

import com.ampairs.communication.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * Channel- and locale-specific rendering of a [MessageTemplate]. For EMAIL: `subject` + `htmlBody`
 * (rich HTML) + optional `textBody` (plain-text alternative). For SMS/WhatsApp/push: `textBody`.
 * `providerTemplateId` + `providerParamsJson` carry provider-approved template references (WhatsApp).
 */
@Entity(name = "message_template_variant")
@Table(
    indexes = [
        Index(name = "idx_variant_template", columnList = "template_uid"),
        Index(name = "idx_variant_owner", columnList = "owner_id"),
    ]
)
class TemplateVariant : OwnableBaseDomain() {

    @Column(name = "template_uid", length = 200, nullable = false)
    var templateUid: String = ""

    @Column(name = "channel", length = 20, nullable = false)
    var channel: String = "EMAIL"

    @Column(name = "locale", length = 16, nullable = false)
    var locale: String = "en"

    @Column(name = "subject", length = 500)
    var subject: String? = null

    @Column(name = "html_body", columnDefinition = "TEXT")
    var htmlBody: String? = null

    @Column(name = "text_body", columnDefinition = "TEXT")
    var textBody: String? = null

    @Column(name = "provider_template_id", length = 200)
    var providerTemplateId: String? = null

    @Column(name = "provider_params_json", columnDefinition = "TEXT")
    var providerParamsJson: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.TEMPLATE_VARIANT_PREFIX
}
