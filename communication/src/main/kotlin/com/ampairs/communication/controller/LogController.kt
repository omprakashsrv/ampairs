package com.ampairs.communication.controller

import com.ampairs.communication.domain.dto.CommunicationLogResponse
import com.ampairs.communication.domain.dto.asResponse
import com.ampairs.communication.repository.CommunicationLogRepository
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Pull-only delivery-status feed for the mobile app (FR-035). Server-authored — there is no POST.
 */
@RestController
@RequestMapping("/communication/v1/logs")
class LogController(
    private val logRepository: CommunicationLogRepository,
) {

    @GetMapping("/sync")
    fun sync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<CommunicationLogResponse>> {
        val property = if (sortBy in setOf("createdAt", "updatedAt", "status")) sortBy else "updatedAt"
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), property))
        val result = if (lastSync.isNullOrBlank()) {
            logRepository.findAllForSync(pageable)
        } else {
            runCatching {
                logRepository.findByUpdatedAtAfter(
                    Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8)), pageable
                )
            }.getOrElse { logRepository.findAllForSync(pageable) }
        }
        return ApiResponse.success(PageResponse.from(result.map { it.asResponse() }))
    }
}
