package com.ampairs.communication.domain.model

import com.ampairs.communication.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * Per-customer × channel × category consent. Transactional is always effectively allowed; promotional
 * opt-out flips `optedIn = false`. Unique `(owner_id, customer_uid, channel, category)`.
 */
@Entity(name = "communication_preference")
@Table(indexes = [Index(name = "idx_pref_owner", columnList = "owner_id")])
class CommunicationPreference : OwnableBaseDomain() {

    @Column(name = "customer_uid", length = 200, nullable = false)
    var customerUid: String = ""

    @Column(name = "channel", length = 20, nullable = false)
    var channel: String = "EMAIL"

    @Column(name = "category", length = 20, nullable = false)
    var category: String = "PROMOTIONAL"

    @Column(name = "opted_in", nullable = false)
    var optedIn: Boolean = true

    @Column(name = "source", length = 40)
    var source: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.PREFERENCE_PREFIX
}
