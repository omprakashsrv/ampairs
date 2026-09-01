package com.ampairs.connector.controller

import com.ampairs.connector.domain.dto.ConfigRequest
import com.ampairs.connector.domain.dto.ConfigResponse
import com.ampairs.connector.domain.dto.ConnectionTestRequest
import com.ampairs.connector.domain.dto.ConnectionTestResponse
import com.ampairs.connector.service.ConnectorConfigService
import com.ampairs.core.domain.dto.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Connection config for an installation. Secrets are write-only (never returned). */
@RestController
@RequestMapping("/connector/v1/installations/{uid}/config")
class ConnectorConfigController(
    private val configService: ConnectorConfigService,
) {
    @GetMapping
    fun get(@PathVariable uid: String): ApiResponse<ConfigResponse> =
        ApiResponse.success(configService.get(uid))

    @PutMapping
    fun upsert(@PathVariable uid: String, @RequestBody request: ConfigRequest): ApiResponse<ConfigResponse> =
        ApiResponse.success(configService.upsert(uid, request))

    /**
     * Records a connection-test result. For client-side connectors the client runs the actual
     * reachability check and reports the outcome here (FR-009).
     */
    @PostMapping("/test")
    fun test(@PathVariable uid: String, @RequestBody request: ConnectionTestRequest): ApiResponse<ConnectionTestResponse> =
        ApiResponse.success(configService.recordConnectionTest(uid, request.ok, request.message))
}
