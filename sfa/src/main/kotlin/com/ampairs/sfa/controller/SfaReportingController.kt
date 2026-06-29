package com.ampairs.sfa.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.sfa.domain.dto.AttendanceSummaryResponse
import com.ampairs.sfa.domain.dto.LeaveRequest
import com.ampairs.sfa.domain.dto.LeaveResponse
import com.ampairs.sfa.domain.dto.VisitProductivityResponse
import com.ampairs.sfa.domain.dto.VisitSurveyResponseRequest
import com.ampairs.sfa.domain.dto.VisitSurveyResponseResponse
import com.ampairs.sfa.domain.dto.asResponse
import com.ampairs.sfa.domain.dto.toEntity
import com.ampairs.sfa.domain.service.LeaveService
import com.ampairs.sfa.domain.service.ReportingService
import com.ampairs.sfa.domain.service.VisitSurveyResponseService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Field-ops reporting (manager) + leave CRUD + offline survey `/sync` (Phase 8b). Summaries are
 * online, server-computed reads; survey responses are captured offline via the canonical contract.
 */
@RestController
@RequestMapping("/sfa/v1")
class SfaReportingController(
    private val leaveService: LeaveService,
    private val reportingService: ReportingService,
    private val visitSurveyResponseService: VisitSurveyResponseService,
) {

    private fun pageable(page: Int, size: Int, sortBy: String, sortDir: String): Pageable =
        PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy))

    // ──────────── leave CRUD ────────────
    @PostMapping("/leaves")
    fun createLeave(@RequestBody @Valid request: LeaveRequest): ApiResponse<LeaveResponse> =
        ApiResponse.success(leaveService.create(request.toEntity()).asResponse())

    @GetMapping("/leaves")
    fun listLeaves(
        @RequestParam("rep_member_uid") repMemberUid: String,
        @RequestParam("from") from: String,
        @RequestParam("to") to: String,
    ): ApiResponse<List<LeaveResponse>> =
        ApiResponse.success(leaveService.list(repMemberUid, Instant.parse(from), Instant.parse(to)).map { it.asResponse() })

    @DeleteMapping("/leaves/{uid}")
    fun deleteLeave(@PathVariable uid: String): ApiResponse<Boolean> =
        ApiResponse.success(leaveService.delete(uid))

    // ──────────── leave /sync ────────────
    @GetMapping("/leaves/sync")
    fun getLeavesSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<LeaveResponse>> {
        val result = leaveService.getLeavesAfterSync(lastSync, pageable(page, size, sortBy, sortDir))
        return ApiResponse.success(PageResponse.from(result) { it.asResponse() })
    }

    @PostMapping("/leaves/sync")
    fun pushLeaves(@RequestBody @Valid request: List<LeaveRequest>): ApiResponse<List<LeaveResponse>> =
        ApiResponse.success(leaveService.bulkUpsertLeaves(request.map { it.toEntity() }).map { it.asResponse() })

    // ──────────── visit surveys /sync ────────────
    @GetMapping("/visit-surveys/sync")
    fun getSurveysSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<VisitSurveyResponseResponse>> {
        val result = visitSurveyResponseService.getAfterSync(lastSync, pageable(page, size, sortBy, sortDir))
        return ApiResponse.success(PageResponse.from(result) { it.asResponse() })
    }

    @PostMapping("/visit-surveys/sync")
    fun pushSurveys(@RequestBody @Valid request: List<VisitSurveyResponseRequest>): ApiResponse<List<VisitSurveyResponseResponse>> =
        ApiResponse.success(visitSurveyResponseService.bulkUpsert(request.map { it.toEntity() }).map { it.asResponse() })

    // ──────────── reporting reads ────────────
    @GetMapping("/attendance/summary")
    fun attendanceSummary(
        @RequestParam("rep_member_uid") repMemberUid: String,
        @RequestParam("from") from: String,
        @RequestParam("to") to: String,
    ): ApiResponse<AttendanceSummaryResponse> =
        ApiResponse.success(reportingService.attendanceSummary(repMemberUid, Instant.parse(from), Instant.parse(to)))

    @GetMapping("/visits/productivity")
    fun visitProductivity(
        @RequestParam("rep_member_uid") repMemberUid: String,
        @RequestParam("from") from: String,
        @RequestParam("to") to: String,
    ): ApiResponse<VisitProductivityResponse> =
        ApiResponse.success(reportingService.visitProductivity(repMemberUid, Instant.parse(from), Instant.parse(to)))
}
