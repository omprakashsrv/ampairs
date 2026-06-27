package com.ampairs.agent.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * The proxied upstream response: the raw body stream plus the headers the controller relays back to
 * the app so download progress and HTTP Range/resume work transparently.
 */
data class UpstreamModelStream(
    val statusCode: Int,
    val body: InputStream,
    val contentLength: Long?,
    val contentRange: String?,
    val acceptRanges: String?,
)

/**
 * Streams an on-device model file from its upstream source (HuggingFace `litert-community`) through
 * the backend, so the app never sees the upstream URL and the team keeps a single control point
 * (auth, logging, future quota). The client's `Range` header is forwarded upstream, so partial /
 * resumed downloads of these multi-GB files Just Work — upstream returns `206` with `Content-Range`
 * and we relay it.
 *
 * Uses the JDK [HttpClient] with [HttpResponse.BodyHandlers.ofInputStream] for a true streaming copy
 * (bounded memory), following redirects (HF `resolve/main` → CDN).
 */
@Service
class AiModelProxyService(
    private val catalogService: AiModelCatalogService,
    // HuggingFace access token for gated repos (e.g. google/gemma-3n-*, litert-community/*). Bound
    // from the `agent.hf-token` property in application.yml, which resolves AGENT_HF_TOKEN (then
    // HF_TOKEN) → blank. Left blank, gated models return 401 upstream → 502 to the app.
    @Value("\${agent.hf-token:}") private val hfToken: String,
) {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    /**
     * Opens an upstream byte stream for [modelId]. Throws 404 if the id is unknown.
     * [rangeHeader] (if present) is forwarded so the app can resume an interrupted download.
     */
    fun open(modelId: String, rangeHeader: String?): UpstreamModelStream {
        val model = catalogService.findById(modelId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown model: $modelId")
        if (model.sourceUrl.isBlank()) {
            // Cloud (server-side) models have no downloadable file — they run via the chat proxy.
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Model is not downloadable: $modelId")
        }

        val requestBuilder = HttpRequest.newBuilder(URI.create(model.sourceUrl))
            .timeout(Duration.ofMinutes(30))
            .GET()
        if (hfToken.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $hfToken")
        }
        if (!rangeHeader.isNullOrBlank()) {
            requestBuilder.header("Range", rangeHeader)
        }

        val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream())

        if (response.statusCode() >= 400) {
            runCatching { response.body().close() }
            val code = response.statusCode()
            val detail = if (code == 401 || code == 403) {
                "Upstream model source requires authentication (gated repo) for $modelId — " +
                    "set AGENT_HF_TOKEN to a HuggingFace token that has accepted the model license"
            } else {
                "Upstream model source returned $code for $modelId"
            }
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, detail)
        }

        val headers = response.headers()
        return UpstreamModelStream(
            statusCode = response.statusCode(),
            body = response.body(),
            contentLength = headers.firstValueAsLong("content-length").let { if (it.isPresent) it.asLong else null },
            contentRange = headers.firstValue("content-range").orElse(null),
            acceptRanges = headers.firstValue("accept-ranges").orElse(null),
        )
    }
}
