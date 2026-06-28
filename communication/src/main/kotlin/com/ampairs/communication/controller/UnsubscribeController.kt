package com.ampairs.communication.controller

import com.ampairs.communication.service.consent.UnsubscribeService
import com.ampairs.core.domain.dto.ApiResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

/**
 * Public, token-scoped unsubscribe (no X-Workspace-ID — tenant comes from the signed token; see the
 * plan's Complexity Tracking for the Principle IV/V exception). The browser GET renders a minimal
 * confirmation page; the JSON POST companion returns the standard ApiResponse envelope.
 *
 * NOTE: this path must be allow-listed in the security config so it is reachable without auth.
 */
@RestController
@RequestMapping("/communication/v1/unsubscribe")
class UnsubscribeController(
    private val unsubscribeService: UnsubscribeService,
) {

    @GetMapping(produces = [MediaType.TEXT_HTML_VALUE])
    fun page(@RequestParam("token") token: String): String {
        val ok = unsubscribeService.process(token)
        return if (ok) {
            "<html><body><h3>You're unsubscribed</h3><p>You will no longer receive promotional messages on this channel.</p></body></html>"
        } else {
            "<html><body><h3>Link expired or invalid</h3></body></html>"
        }
    }

    @PostMapping
    fun unsubscribe(@RequestBody body: Map<String, String>): ApiResponse<Map<String, Boolean>> {
        val token = body["token"].orEmpty()
        return ApiResponse.success(mapOf("applied" to unsubscribeService.process(token)))
    }
}
