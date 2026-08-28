package com.ampairs.connector.controller

import com.ampairs.connector.domain.dto.CatalogueConnectorResponse
import com.ampairs.connector.domain.dto.asResponse
import com.ampairs.connector.service.ConnectorCatalogueService
import com.ampairs.core.domain.dto.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Apps & Extensions catalogue. Tenant context is set by `SessionUserFilter` from `X-Workspace-ID`.
 */
@RestController
@RequestMapping("/connector/v1/catalogue")
class ConnectorCatalogueController(
    private val catalogueService: ConnectorCatalogueService,
) {
    /** List connectors available to this workspace, flagging the already-installed ones (FR-001). */
    @GetMapping
    fun list(): ApiResponse<List<CatalogueConnectorResponse>> {
        val items = catalogueService.listAvailable().map { (connector, installed) ->
            connector.asResponse(installed)
        }
        return ApiResponse.success(items)
    }
}
