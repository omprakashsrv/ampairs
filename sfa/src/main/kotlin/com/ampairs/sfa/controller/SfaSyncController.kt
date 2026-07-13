package com.ampairs.sfa.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.sfa.domain.dto.AdherenceSummary
import com.ampairs.sfa.domain.dto.AttendanceRequest
import com.ampairs.sfa.domain.dto.AttendanceResponse
import com.ampairs.sfa.domain.dto.BeatOutletRequest
import com.ampairs.sfa.domain.dto.BeatOutletResponse
import com.ampairs.sfa.domain.dto.BeatRequest
import com.ampairs.sfa.domain.dto.BeatResponse
import com.ampairs.sfa.domain.dto.FieldOrderRequest
import com.ampairs.sfa.domain.dto.FieldOrderResponse
import com.ampairs.sfa.domain.dto.JourneyPlanRequest
import com.ampairs.sfa.domain.dto.JourneyPlanResponse
import com.ampairs.sfa.domain.dto.PlannedVisitRequest
import com.ampairs.sfa.domain.dto.PlannedVisitResponse
import com.ampairs.sfa.domain.dto.VisitRequest
import com.ampairs.sfa.domain.dto.VisitResponse
import com.ampairs.sfa.domain.dto.asResponse
import com.ampairs.sfa.domain.dto.toEntity
import com.ampairs.sfa.domain.service.AttendanceService
import com.ampairs.sfa.domain.service.BeatService
import com.ampairs.sfa.domain.service.FieldOrderService
import com.ampairs.sfa.domain.service.JourneyPlanService
import com.ampairs.sfa.domain.service.VisitService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Offline-first SFA `/sync` surface (FIELD_REP / manager, distributor-scoped). Every entity follows
 * the canonical `GET`/`POST /sfa/v1/{resource}/sync` contract; `/sfa/v1/adherence` is a read-model.
 * Tenant is set upstream by `SessionUserFilter` from `X-Workspace-ID`.
 */
@RestController
@RequestMapping("/sfa/v1")
class SfaSyncController(
    private val beatService: BeatService,
    private val journeyPlanService: JourneyPlanService,
    private val visitService: VisitService,
    private val attendanceService: AttendanceService,
    private val fieldOrderService: FieldOrderService,
) {

    private fun pageable(page: Int, size: Int, sortBy: String, sortDir: String): Pageable =
        PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy))

    // ──────────── beats ────────────
    @GetMapping("/beats/sync")
    fun getBeats(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<BeatResponse>> {
        val result = beatService.getBeatsAfterSync(lastSync, pageable(page, size, sortBy, sortDir))
        return ApiResponse.success(PageResponse.from(result) { it.asResponse() })
    }

    @PostMapping("/beats/sync")
    fun pushBeats(@RequestBody @Valid request: List<BeatRequest>): ApiResponse<List<BeatResponse>> =
        ApiResponse.success(beatService.bulkUpsertBeats(request.map { it.toEntity() }).map { it.asResponse() })

    // ──────────── beat outlets ────────────
    @GetMapping("/beat-outlets/sync")
    fun getBeatOutlets(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<BeatOutletResponse>> {
        val result = beatService.getBeatOutletsAfterSync(lastSync, pageable(page, size, sortBy, sortDir))
        return ApiResponse.success(PageResponse.from(result) { it.asResponse() })
    }

    @PostMapping("/beat-outlets/sync")
    fun pushBeatOutlets(@RequestBody @Valid request: List<BeatOutletRequest>): ApiResponse<List<BeatOutletResponse>> =
        ApiResponse.success(beatService.bulkUpsertBeatOutlets(request.map { it.toEntity() }).map { it.asResponse() })

    // ──────────── journey plans ────────────
    @GetMapping("/journey-plans/sync")
    fun getJourneyPlans(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<JourneyPlanResponse>> {
        val result = journeyPlanService.getJourneyPlansAfterSync(lastSync, pageable(page, size, sortBy, sortDir))
        return ApiResponse.success(PageResponse.from(result) { it.asResponse() })
    }

    @PostMapping("/journey-plans/sync")
    fun pushJourneyPlans(@RequestBody @Valid request: List<JourneyPlanRequest>): ApiResponse<List<JourneyPlanResponse>> =
        ApiResponse.success(journeyPlanService.bulkUpsertJourneyPlans(request.map { it.toEntity() }).map { it.asResponse() })

    // ──────────── planned visits ────────────
    @GetMapping("/planned-visits/sync")
    fun getPlannedVisits(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<PlannedVisitResponse>> {
        val result = journeyPlanService.getPlannedVisitsAfterSync(lastSync, pageable(page, size, sortBy, sortDir))
        return ApiResponse.success(PageResponse.from(result) { it.asResponse() })
    }

    @PostMapping("/planned-visits/sync")
    fun pushPlannedVisits(@RequestBody @Valid request: List<PlannedVisitRequest>): ApiResponse<List<PlannedVisitResponse>> =
        ApiResponse.success(journeyPlanService.bulkUpsertPlannedVisits(request.map { it.toEntity() }).map { it.asResponse() })

    // ──────────── visits ────────────
    @GetMapping("/visits/sync")
    fun getVisits(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<VisitResponse>> {
        val result = visitService.getVisitsAfterSync(lastSync, pageable(page, size, sortBy, sortDir))
        return ApiResponse.success(PageResponse.from(result) { it.asResponse() })
    }

    @PostMapping("/visits/sync")
    fun pushVisits(@RequestBody @Valid request: List<VisitRequest>): ApiResponse<List<VisitResponse>> =
        ApiResponse.success(visitService.bulkUpsertVisits(request.map { it.toEntity() }).map { it.asResponse() })

    // ──────────── attendance ────────────
    @GetMapping("/attendance/sync")
    fun getAttendance(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<AttendanceResponse>> {
        val result = attendanceService.getAttendanceAfterSync(lastSync, pageable(page, size, sortBy, sortDir))
        return ApiResponse.success(PageResponse.from(result) { it.asResponse() })
    }

    @PostMapping("/attendance/sync")
    fun pushAttendance(@RequestBody @Valid request: List<AttendanceRequest>): ApiResponse<List<AttendanceResponse>> =
        ApiResponse.success(attendanceService.bulkUpsertAttendance(request.map { it.toEntity() }).map { it.asResponse() })

    // ──────────── field orders ────────────
    @GetMapping("/field-orders/sync")
    fun getFieldOrders(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<FieldOrderResponse>> {
        val result = fieldOrderService.getFieldOrdersAfterSync(lastSync, pageable(page, size, sortBy, sortDir))
        return ApiResponse.success(PageResponse.from(result) { it.asResponse() })
    }

    @PostMapping("/field-orders/sync")
    fun pushFieldOrders(@RequestBody @Valid request: List<FieldOrderRequest>): ApiResponse<List<FieldOrderResponse>> =
        ApiResponse.success(fieldOrderService.bulkUpsertFieldOrders(request.map { it.toEntity() }).map { it.asResponse() })

    // ──────────── adherence (read-model) ────────────
    @GetMapping("/adherence")
    fun adherence(
        @RequestParam("rep_member_uid") repMemberUid: String,
        @RequestParam("period_from") periodFrom: String,
        @RequestParam("period_to") periodTo: String,
    ): ApiResponse<AdherenceSummary> =
        ApiResponse.success(journeyPlanService.adherence(repMemberUid, Instant.parse(periodFrom), Instant.parse(periodTo)))
}
