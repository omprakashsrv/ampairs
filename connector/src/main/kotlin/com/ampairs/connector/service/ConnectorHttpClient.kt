package com.ampairs.connector.service

import com.ampairs.connector.config.ConnectorJson
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/** One outbound fetch: fully-resolved URL, headers, and where in the JSON body the record array lives. */
data class HttpFetchRequest(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    /** Dotted path to the array of records inside the response body (e.g. "data.items"). Null/blank = the body itself. */
    val recordsPath: String? = null,
)

/**
 * Thin outbound HTTP/JSON client for SERVER-SIDE connectors. Fetches a remote endpoint and returns the
 * record array as `List<Map>`. Deliberately minimal (GET + header auth + JSON path) — vendor providers
 * add their own pagination/rate-limit handling on top. Header VALUES may carry secrets, so this class
 * never logs them (only the URL and record count).
 */
@Component
class ConnectorHttpClient {
    private val log = LoggerFactory.getLogger(ConnectorHttpClient::class.java)
    private val mapper = ConnectorJson.mapper
    private val recordType = object : TypeReference<Map<String, Any?>>() {}
    private val restClient: RestClient = RestClient.create()

    /** GET [request].url with headers, parse JSON, return the records at [request].recordsPath. */
    fun fetchRecords(request: HttpFetchRequest): List<Map<String, Any?>> {
        val body = restClient.get()
            .uri(request.url)
            .headers { h -> request.headers.forEach { (k, v) -> h.set(k, v) } }
            .retrieve()
            .body(String::class.java)
            ?: return emptyList()

        val root = mapper.readTree(body)
        val recordsNode = navigate(root, request.recordsPath)
        val records: List<Map<String, Any?>> = when {
            recordsNode == null || recordsNode.isMissingNode || recordsNode.isNull -> emptyList()
            recordsNode.isArray -> recordsNode.map { mapper.convertValue(it, recordType) }
            recordsNode.isObject -> listOf(mapper.convertValue(recordsNode, recordType))
            else -> emptyList()
        }
        log.info("Connector HTTP fetch {} → {} record(s)", request.url, records.size)
        return records
    }

    /** Lightweight reachability probe for the server-side connection test (HTTP 2xx = reachable). */
    fun probe(url: String, headers: Map<String, String>): Boolean = runCatching {
        restClient.get()
            .uri(url)
            .headers { h -> headers.forEach { (k, v) -> h.set(k, v) } }
            .retrieve()
            .toBodilessEntity()
        true
    }.getOrElse {
        log.warn("Connector HTTP probe failed for {}: {}", url, it.message)
        false
    }

    private fun navigate(root: JsonNode, path: String?): JsonNode? {
        val trimmed = path?.trim().orEmpty()
        if (trimmed.isEmpty()) return root
        var node: JsonNode = root
        for (segment in trimmed.split('.')) {
            node = node.path(segment)
            if (node.isMissingNode) return null
        }
        return node
    }
}
