package com.ampairs.communication.domain.model

import com.ampairs.communication.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * Reusable, categorized message template (aggregate root). Variants (per channel × locale) are
 * stored separately and keyed by `template_uid`; the service composes the aggregate for `/sync`.
 * `category` is TRANSACTIONAL or PROMOTIONAL (see [com.ampairs.communication.domain.enums.MessageCategory]).
 */
@Entity(name = "message_template")
@Table(indexes = [Index(name = "idx_message_template_owner", columnList = "owner_id")])
class MessageTemplate : OwnableBaseDomain() {

    @Column(name = "code", length = 120, nullable = false)
    var code: String = ""

    @Column(name = "name", length = 200, nullable = false)
    var name: String = ""

    @Column(name = "category", length = 20, nullable = false)
    var category: String = "TRANSACTIONAL"

    @Column(name = "default_locale", length = 16, nullable = false)
    var defaultLocale: String = "en"

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

    /** Optimistic-concurrency counter for the aggregate `/sync` (mirrors form's base_version). */
    @Column(name = "base_version", nullable = false)
    var baseVersion: Int = 1

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.TEMPLATE_PREFIX
}
