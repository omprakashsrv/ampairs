package com.ampairs.communication.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.notification.credential.CredentialRequest
import com.ampairs.notification.credential.CredentialResponse
import com.ampairs.notification.credential.WorkspaceChannelCredentialService
import jakarta.validation.Valid
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

/**
 * Workspace provider-credential management (the client's own sender identity). Secrets are
 * write-only — accepted on create/update, NEVER returned (responses are masked). Owned by the
 * notification module; surfaced here under the communication settings namespace. Tenant set by
 * SessionUserFilter (X-Workspace-ID).
 */
@RestController
@RequestMapping("/communication/v1/credentials")
@Validated
class CredentialController(
    private val credentialService: WorkspaceChannelCredentialService,
) {

    @GetMapping
    fun list(): ApiResponse<List<CredentialResponse>> = ApiResponse.success(credentialService.list())

    @PostMapping
    fun create(@RequestBody @Valid request: CredentialRequest): ApiResponse<CredentialResponse> =
        ApiResponse.success(credentialService.upsert(request))

    @PutMapping("/{uid}")
    fun update(
        @PathVariable uid: String,
        @RequestBody @Valid request: CredentialRequest,
    ): ApiResponse<CredentialResponse> =
        ApiResponse.success(credentialService.upsert(request.copy(uid = uid)))

    @DeleteMapping("/{uid}")
    fun delete(@PathVariable uid: String): ApiResponse<Unit> {
        credentialService.delete(uid)
        return ApiResponse.success(Unit)
    }

    @PostMapping("/{uid}/validate")
    fun validate(@PathVariable uid: String): ApiResponse<CredentialResponse> =
        ApiResponse.success(credentialService.validate(uid))
}
