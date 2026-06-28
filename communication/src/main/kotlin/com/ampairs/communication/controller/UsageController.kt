package com.ampairs.communication.controller

import com.ampairs.communication.domain.dto.UsageReportResponse
import com.ampairs.communication.service.usage.UsageReportService
import com.ampairs.core.domain.dto.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Usage/billing report aggregated by channel × credential × billing mode for a period. */
@RestController
@RequestMapping("/communication/v1/usage")
class UsageController(
    private val usageReportService: UsageReportService,
) {

    @GetMapping
    fun report(
        @RequestParam("from", required = false) from: String?,
        @RequestParam("to", required = false) to: String?,
    ): ApiResponse<UsageReportResponse> {
        val toInstant = to?.let { Instant.parse(it) } ?: Instant.now()
        val fromInstant = from?.let { Instant.parse(it) } ?: toInstant.minus(30, ChronoUnit.DAYS)
        return ApiResponse.success(usageReportService.report(fromInstant, toInstant))
    }
}
