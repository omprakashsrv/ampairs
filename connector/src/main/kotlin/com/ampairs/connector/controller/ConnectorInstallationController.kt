package com.ampairs.connector.controller

import com.ampairs.connector.domain.dto.InstallConnectorRequest
import com.ampairs.connector.domain.dto.InstallationResponse
import com.ampairs.connector.domain.dto.asResponse
import com.ampairs.connector.domain.dto.asResponses
import com.ampairs.connector.service.ConnectorInstallationService
import com.ampairs.core.domain.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Per-workspace connector installations. Business exceptions (already-installed, not-found) bubble to
 * the global handler — no try/catch here (Constitution Principles V/VI).
 */
@RestController
@RequestMapping("/connector/v1/installations")
class ConnectorInstallationController(
    private val installationService: ConnectorInstallationService,
) {
    @GetMapping
    fun list(): ApiResponse<List<InstallationResponse>> =
        ApiResponse.success(installationService.listInstallations().asResponses())

    @PostMapping
    fun install(@RequestBody @Valid request: InstallConnectorRequest): ApiResponse<InstallationResponse> =
        ApiResponse.success(installationService.install(request.connectorType).asResponse())

    @DeleteMapping("/{uid}")
    fun uninstall(@PathVariable uid: String): ApiResponse<Unit> {
        installationService.uninstall(uid)
        return ApiResponse.success(Unit)
    }
}
