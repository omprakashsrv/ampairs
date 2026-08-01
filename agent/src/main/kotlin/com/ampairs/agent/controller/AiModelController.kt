package com.ampairs.agent.controller

import com.ampairs.agent.domain.dto.AiModelResponse
import com.ampairs.agent.domain.dto.asResponses
import com.ampairs.agent.service.AiModelCatalogService
import com.ampairs.agent.service.AiModelProxyService
import com.ampairs.core.domain.dto.ApiResponse
import java.io.IOException
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.async.AsyncRequestTimeoutException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.nio.charset.StandardCharsets

/**
 * On-device AI model catalog + download proxy. Global (not tenant-scoped) reference data — still
 * JWT-authenticated via `SecurityConfiguration` (`anyRequest().authenticated()`). The app always
 * carries `X-Workspace-ID` from inside a workspace, so no `SessionUserFilter` exemption is needed;
 * the endpoints simply don't use tenant context.
 *
 * - `GET /agent/v1/models` — the manifest the app's model-picker renders.
 * - `GET /agent/v1/models/{id}/download` — streams the `.litertlm` bytes from the upstream source
 *   (kept opaque to the client). Forwards `Range` for resumable, multi-GB downloads.
 */
@RestController
@RequestMapping("/agent/v1/models")
class AiModelController(
    private val catalogService: AiModelCatalogService,
    private val proxyService: AiModelProxyService,
) {

    /** The full curated catalog. The app shows all entries and disables what the device can't run. */
    @GetMapping
    fun manifest(): ApiResponse<List<AiModelResponse>> =
        ApiResponse.success(catalogService.listModels().asResponses())

    /** Streaming download proxy. Relays upstream status (200/206) + range headers for resume. */
    @GetMapping("/{modelId}/download")
    fun download(
        @PathVariable modelId: String,
        @RequestHeader(value = HttpHeaders.RANGE, required = false) range: String?,
    ): ResponseEntity<StreamingResponseBody> {
        val model = catalogService.findById(modelId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown model: $modelId")

        val upstream = proxyService.open(modelId, range)
        val body = StreamingResponseBody { output ->
            try {
                upstream.body.use { it.copyTo(output, DEFAULT_COPY_BUFFER) }
            } catch (e: IOException) {
                // The client aborted or the network dropped mid-stream — normal for multi-GB,
                // resumable downloads (the app retries with a Range header). Headers are already
                // committed, so there is nothing to report; swallow it instead of letting it bubble
                // to GlobalExceptionHandler, which can't serialize an ApiResponse into the
                // already-open application/octet-stream response.
                if (e.cause is InterruptedException) Thread.currentThread().interrupt()
                runCatching { upstream.body.close() }
            }
        }

        val builder = ResponseEntity.status(upstream.statusCode)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(model.fileName, StandardCharsets.UTF_8).build().toString(),
            )
            .header(HttpHeaders.ACCEPT_RANGES, upstream.acceptRanges ?: "bytes")
        upstream.contentLength?.let { builder.header(HttpHeaders.CONTENT_LENGTH, it.toString()) }
        upstream.contentRange?.let { builder.header(HttpHeaders.CONTENT_RANGE, it) }
        return builder.body(body)
    }

    /**
     * A download stream that exceeded the async window or whose client went away after the response
     * was committed. The octet-stream response is already open, so an ApiResponse error body can't be
     * written — swallow it here (controller-local handlers win over GlobalExceptionHandler) so it
     * doesn't cascade into "No converter for ApiResponse with preset Content-Type". The app resumes
     * via a Range request.
     */
    @ExceptionHandler(AsyncRequestTimeoutException::class)
    fun handleAsyncTimeout() {
        // no-op: response already committed; nothing to return
    }

    companion object {
        private const val DEFAULT_COPY_BUFFER = 64 * 1024
    }
}
