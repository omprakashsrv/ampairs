package com.ampairs.customer.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.customer.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity(name = "state")
@Table(indexes = arrayOf(Index(name = "state_name_idx", columnList = "name,owner_id", unique = true)))
class State : OwnableBaseDomain() {

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "short_name", nullable = false, length = 6)
    var shortName: String = ""

    @Column(name = "gst_code", nullable = false)
    var gstCode: Int = 0

    @Column(name = "active", nullable = false)
    var active: Boolean = true
    override fun obtainIdPrefix(): String {
        return Constants.STATE_PREFIX
    }
}