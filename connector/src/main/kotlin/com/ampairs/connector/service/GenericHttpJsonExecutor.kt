package com.ampairs.connector.service

import com.ampairs.connector.domain.catalogue.ConnectorCatalogueRegistry
import com.ampairs.connector.domain.dto.FieldMappingRuleDto
import com.ampairs.connector.domain.dto.SparseUpsertRow
import com.ampairs.connector.domain.dto.SyncCheckpointDto
import com.ampairs.connector.domain.model.ConnectorInstallation
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * SERVER-SIDE executor for the Generic HTTP/JSON connector — the first server-hosted connector and the
 * template every future vendor provider (Zoho/Shopify/QuickBooks) copies. For each supported entity
 * with a configured `path.{entity}` and a mapping, it GETs the remote endpoint, applies the stored
 * field mapping to each record (presence-preserving), and writes via [ConnectorSparseUpsertService] —
 * inheriting the data-loss-safe presence ∩ allowlist guarantee. Per-entity checkpoints advance on
 * success only.
 *
 * Config contract (see [com.ampairs.connector.domain.catalogue.GenericHttpJsonConnectorProvider]):
 *   non-secret: base_url, auth_header (default "Authorization"), auth_scheme (default "Bearer"),
 *               path.{entity}, records_path.{entity} (optional), since_param.{entity} (optional)
 *   secret:     api_key
 */
@Component
class GenericHttpJsonExecutor(
    private val registry: ConnectorCatalogueRegistry,
    private val configService: ConnectorConfigService,
    private val mappingService: ConnectorMappingService,
    private val sparseUpsertService: ConnectorSparseUpsertService,
    private val syncStateService: ConnectorSyncStateService,
    private val httpClient: ConnectorHttpClient,
) : ServerSideConnectorSyncExecutor {

    private val log = LoggerFactory.getLogger(GenericHttpJsonExecutor::class.java)

    override val connectorType: String = CONNECTOR_TYPE

    override fun runCycle(installation: ConnectorInstallation): ServerSyncOutcome {
        val installationUid = installation.uid
        val config = configService.resolveForExecution(installationUid)
        val baseUrl = config.value("base_url")
            ?: return ServerSyncOutcome(listOf(EntitySyncOutcome("*", error = "Missing base_url")))
        val headers = authHeaders(config)

        val supported = registry.find(connectorType)?.supportedEntities ?: emptyList()
        val mappingsByEntity = mappingService.list(installationUid).associateBy { it.entityType }
        val checkpoints = syncStateService.listCheckpoints(installationUid)
            .associateBy { it.entityType }

        val outcomes = supported.mapNotNull { entity ->
            val path = config.value("path.$entity") ?: return@mapNotNull null // entity not exposed by this config
            val rules = mappingsByEntity[entity]?.rules?.filterNot { it.unmapped }.orEmpty()
            if (rules.isEmpty()) return@mapNotNull null // nothing to write for this entity
            syncEntity(installationUid, entity, baseUrl, path, headers, rules, config, checkpoints[entity]?.watermark)
        }
        return ServerSyncOutcome(outcomes)
    }

    override fun testConnection(installation: ConnectorInstallation): Boolean {
        val config = configService.resolveForExecution(installation.uid)
        val baseUrl = config.value("base_url") ?: return false
        // Probe the first configured entity path if any (more meaningful than a bare host), else base URL.
        val supported = registry.find(connectorType)?.supportedEntities ?: emptyList()
        val firstPath = supported.firstNotNullOfOrNull { config.value("path.$it") }
        val url = if (firstPath != null) buildUrl(baseUrl, firstPath, null, null) else baseUrl.trimEnd('/')
        return httpClient.probe(url, authHeaders(config))
    }

    /** Build the auth header map from config (header name/scheme + secret api key). Empty if no key. */
    private fun authHeaders(config: ResolvedConnectorConfig): Map<String, String> {
        val apiKey = config.secret("api_key") ?: return emptyMap()
        val authHeader = config.value("auth_header") ?: "Authorization"
        val authScheme = config.value("auth_scheme") ?: "Bearer"
        return mapOf(authHeader to listOfNotNull(authScheme.ifBlank { null }, apiKey).joinToString(" "))
    }

    private fun syncEntity(
        installationUid: String,
        entity: String,
        baseUrl: String,
        path: String,
        headers: Map<String, String>,
        rules: List<FieldMappingRuleDto>,
        config: ResolvedConnectorConfig,
        priorWatermark: String?,
    ): EntitySyncOutcome {
        return try {
            val url = buildUrl(baseUrl, path, config.value("since_param.$entity"), priorWatermark)
            val cycleStart = Instant.now().toString()
            val records = httpClient.fetchRecords(
                HttpFetchRequest(url = url, headers = headers, recordsPath = config.value("records_path.$entity")),
            )
            val rows = records.mapNotNull { toRow(it, rules) }
            val results = if (rows.isEmpty()) emptyList()
            else sparseUpsertService.upsert(installationUid, entity, rows, trigger = "SCHEDULED")

            // Advance the checkpoint only after a clean cycle for this entity (no failed rows).
            val failed = results.count { it.outcome == "FAILED" }
            if (failed == 0) {
                syncStateService.upsertCheckpoint(
                    installationUid,
                    SyncCheckpointDto(entityType = entity, direction = "INBOUND", watermark = cycleStart),
                )
            }
            EntitySyncOutcome(
                entityType = entity,
                processed = rows.size,
                created = results.count { it.outcome == "CREATED" },
                updated = results.count { it.outcome == "UPDATED" },
                failed = failed,
            )
        } catch (e: Exception) {
            log.warn("Generic HTTP/JSON sync failed for entity '{}' (installation {}): {}", entity, installationUid, e.message)
            EntitySyncOutcome(entityType = entity, error = e.message ?: e::class.simpleName)
        }
    }

    /** Map ONE remote record → a sparse row using the mapping rules. Key presence is preserved: only
     *  external fields actually present in the record produce a mapped column. */
    private fun toRow(record: Map<String, Any?>, rules: List<FieldMappingRuleDto>): SparseUpsertRow? {
        var refId: String? = null
        val values = LinkedHashMap<String, Any?>()
        for (rule in rules) {
            if (!record.containsKey(rule.externalField)) continue // omitted → leave untouched (sparse)
            val transformed = applyTransform(rule.transform, record[rule.externalField])
            if (rule.ampairsField == "refId") refId = transformed?.toString()
            else values[rule.ampairsField] = transformed
        }
        // A row with no identity can't be matched/created safely → skip it.
        if (refId.isNullOrBlank()) return null
        return SparseUpsertRow(refId = refId, values = values)
    }

    /** Minimal, generic transforms. Unknown transforms pass the value through unchanged. */
    private fun applyTransform(transform: String?, raw: Any?): Any? {
        if (raw == null) return null
        return when (transform?.trim()?.lowercase()) {
            null, "", "identity" -> raw
            "trim" -> raw.toString().trim()
            "upper", "uppercase" -> raw.toString().uppercase()
            "lower", "lowercase" -> raw.toString().lowercase()
            "number", "parse_number" -> raw.toString().trim().toBigDecimalOrNull() ?: raw
            "boolean", "parse_boolean" -> raw.toString().trim().equals("true", ignoreCase = true) ||
                raw.toString().trim() == "1"
            else -> raw // unknown hint: passthrough (don't drop data)
        }
    }

    private fun buildUrl(baseUrl: String, path: String, sinceParam: String?, watermark: String?): String {
        val base = baseUrl.trimEnd('/') + "/" + path.trimStart('/')
        if (sinceParam.isNullOrBlank() || watermark.isNullOrBlank()) return base
        val sep = if (base.contains('?')) '&' else '?'
        return "$base$sep$sinceParam=$watermark"
    }

    companion object {
        const val CONNECTOR_TYPE = "generic_http_json"
    }
}
