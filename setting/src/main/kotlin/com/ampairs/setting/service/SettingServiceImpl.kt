package com.ampairs.setting.service

import com.ampairs.core.exception.BusinessException
import com.ampairs.core.setting.InstalledModulesProvider
import com.ampairs.core.setting.SettingDefinition
import com.ampairs.core.setting.SettingDefinitionProvider
import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.core.sync.EntityChangeType
import com.ampairs.setting.config.Constants
import com.ampairs.setting.domain.dto.SettingRequest
import com.ampairs.setting.domain.model.StoreSetting
import com.ampairs.setting.repository.StoreSettingRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

@Service
class SettingServiceImpl(
    private val repository: StoreSettingRepository,
    private val definitionProviders: List<SettingDefinitionProvider>,
    private val installedModulesProvider: InstalledModulesProvider,
    private val entityChangePublisher: EntityChangePublisher,
) : SettingService {

    /** All declared definitions keyed by "module/key". Definitions live in code, not the DB. */
    private val definitions: Map<String, SettingDefinition> by lazy {
        definitionProviders
            .flatMap { it.definitions() }
            .associateBy { it.identity() }
    }

    @Transactional(readOnly = true)
    override fun getSettingsAfterSync(lastSync: String?, pageable: Pageable): Page<StoreSetting> {
        if (lastSync.isNullOrBlank()) {
            return repository.findAllOrderByUpdatedAt(pageable)
        }
        val instant = runCatching {
            Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8))
        }.getOrNull() ?: return repository.findAllOrderByUpdatedAt(pageable)
        return repository.findByUpdatedAtAfter(instant, pageable)
    }

    @Transactional
    override fun upsertSettings(requests: List<SettingRequest>): List<StoreSetting> =
        requests.map { upsertOne(it) }

    @Transactional(readOnly = true)
    override fun listDefinitions(): List<SettingDefinition> {
        val enabled = installedModulesProvider.enabledModuleCodes()
        return definitions.values.filter { it.requiresModule == null || it.requiresModule in enabled }
    }

    private fun upsertOne(request: SettingRequest): StoreSetting {
        val module = request.module.trim()
        val key = request.key.trim()
        val definition = definitions["$module/$key"]
            ?: throw BusinessException("SETTING_UNKNOWN", "Unknown setting: $module/$key")

        request.value?.let {
            if (!definition.accepts(it)) {
                throw BusinessException(
                    "SETTING_INVALID_VALUE",
                    "Invalid value '$it' for ${definition.identity()} (${definition.valueType})",
                )
            }
        }

        val existing = request.uid?.takeIf { it.isNotBlank() }?.let { repository.findByUid(it) }
            ?: repository.findByModuleCodeAndSettingKey(module, key)

        val entity = (existing ?: StoreSetting()).apply {
            request.uid?.takeIf { it.isNotBlank() }?.let { uid = it }
            moduleCode = module
            settingKey = key
            value = request.value
            valueType = definition.valueType
            active = request.active
            request.refId?.let { refId = it }
        }

        val saved = repository.save(entity)
        entityChangePublisher.publish(
            Constants.SYNC_ENTITY_CODE,
            saved.uid,
            if (!saved.active) EntityChangeType.DELETED else EntityChangeType.UPDATED,
        )
        return saved
    }

    @Transactional(readOnly = true)
    override fun getString(module: String, key: String): String? {
        val stored = repository.findByModuleCodeAndSettingKey(module, key)
        if (stored != null && stored.active) return stored.value
        return definitions["$module/$key"]?.defaultValue
    }

    @Transactional(readOnly = true)
    override fun getBoolean(module: String, key: String): Boolean =
        getString(module, key)?.equals("true", ignoreCase = true) ?: false
}
