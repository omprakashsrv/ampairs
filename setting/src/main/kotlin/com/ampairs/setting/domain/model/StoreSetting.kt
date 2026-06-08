package com.ampairs.setting.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.core.setting.SettingValueType
import com.ampairs.setting.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table

/**
 * A workspace-scoped override of a module setting. One row per `(owner_id, module_code, setting_key)`.
 * Values are stored as text and interpreted via [valueType]. Soft-deleted via [active] = false so the
 * offline clients can reconcile removals during incremental pull.
 */
@Entity(name = "store_setting")
@NamedEntityGraph(name = "StoreSetting.basic")
@Table(
    indexes = [
        Index(name = "idx_store_setting_uid", columnList = "uid", unique = true),
        Index(name = "idx_store_setting_owner", columnList = "owner_id"),
        Index(name = "idx_store_setting_owner_module_key", columnList = "owner_id, module_code, setting_key", unique = true),
        Index(name = "idx_store_setting_updated", columnList = "updated_at"),
    ]
)
class StoreSetting : OwnableBaseDomain() {

    @Column(name = "module_code", length = 64, nullable = false)
    var moduleCode: String = ""

    @Column(name = "setting_key", length = 128, nullable = false)
    var settingKey: String = ""

    @Column(name = "value", columnDefinition = "TEXT")
    var value: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", length = 16, nullable = false)
    var valueType: SettingValueType = SettingValueType.STRING

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.STORE_SETTING_PREFIX
}
