package com.ampairs.setting.service

import com.ampairs.core.setting.SettingDefinition
import com.ampairs.setting.domain.dto.SettingRequest
import com.ampairs.setting.domain.model.StoreSetting
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Workspace settings, exposed for offline bulk sync (pull/push) and for cross-module reads.
 * Other backend modules should depend on this interface — never on the repository — to read a
 * workspace's effective setting value (stored override falling back to the declared default).
 */
interface SettingService {

    /** Incremental pull for the current workspace; includes inactive rows so deletes propagate. */
    fun getSettingsAfterSync(lastSync: String?, pageable: Pageable): Page<StoreSetting>

    /** Bulk push: validate each request against its definition and upsert by uid or (module, key). */
    fun upsertSettings(requests: List<SettingRequest>): List<StoreSetting>

    /** Declared setting definitions applicable to the current workspace's installed modules. */
    fun listDefinitions(): List<SettingDefinition>

    /** Effective string value for the current workspace, or the declared default when no override. */
    fun getString(module: String, key: String): String?

    /** Effective boolean value; falls back to the declared default. */
    fun getBoolean(module: String, key: String): Boolean
}
