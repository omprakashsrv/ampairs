package com.ampairs.connector.controller

import com.ampairs.connector.domain.dto.ConfigRequest
import com.ampairs.connector.domain.dto.ConfigResponse
import com.ampairs.connector.domain.dto.ConnectionTestRequest
import com.ampairs.connector.domain.dto.ConnectionTestResponse
import com.ampairs.connector.service.ConnectorConfigService
import com.ampairs.connector.service.ConnectorConnectionTester
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
    private val connectionTester: ConnectorConnectionTester,
) {
    @GetMapping
    fun get(@PathVariable uid: String): ApiResponse<ConfigResponse> =
        ApiResponse.success(configService.get(uid))

    @PutMapping
    fun upsert(@PathVariable uid: String, @RequestBody request: ConfigRequest): ApiResponse<ConfigResponse> =
        ApiResponse.success(configService.upsert(uid, request))

    /**
     * Connection test. For CLIENT_SIDE connectors the client runs the reachability check and its result
     * is recorded (FR-009); for SERVER_SIDE connectors the backend performs the probe (FR-S06).
     */
    @PostMapping("/test")
    fun test(@PathVariable uid: String, @RequestBody request: ConnectionTestRequest): ApiResponse<ConnectionTestResponse> =
        ApiResponse.success(connectionTester.test(uid, request))
}
