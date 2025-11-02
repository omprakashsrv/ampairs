package com.ampairs.customer.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.customer.config.Constants
import jakarta.persistence.*

@Entity(name = "state")
@Table(
    indexes = arrayOf(
        Index(name = "state_name_idx", columnList = "name,owner_id", unique = true),
        Index(name = "state_master_idx", columnList = "master_state_id")
    )
)
class State : OwnableBaseDomain() {

    @Column(name = "name", nullable = false, length = 100)
    var name: String = ""

    @Column(name = "short_name", nullable = false, length = 6)
    var shortName: String = ""

    @Column(name = "country", nullable = false, length = 100)
    var country: String = ""

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    /**
     * Reference to the master state this workspace state was imported from
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_state_id", referencedColumnName = "uid")
    var masterState: MasterState? = null

    /**
     * Master state code for reference (denormalized for performance)
     */
    @Column(name = "master_state_code", length = 10)
    var masterStateCode: String? = null

    /**
     * Display order for sorting states in workspace
     */
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    override fun obtainSeqIdPrefix(): String {
        return Constants.STATE_PREFIX
    }

    /**
     * Import state data from master state
     */
    fun importFromMasterState(masterState: MasterState) {
        this.masterState = masterState
        this.masterStateCode = masterState.stateCode
        this.name = masterState.name
        this.shortName = masterState.shortName
        this.country = masterState.countryName
        this.active = true
    }

    /**
     * Check if this state is synced with master state
     */
    fun isSyncedWithMaster(): Boolean {
        return masterState != null &&
                name == masterState?.name &&
                shortName == masterState?.shortName &&
                country == masterState?.countryName
    }
}