package com.ampairs.cb_maintenance.service

import com.ampairs.cb_employee.domain.dto.EmployeeResponse
import com.ampairs.cb_employee.service.EmployeeService
import com.ampairs.cb_maintenance.domain.dto.PmEntryRequest
import com.ampairs.cb_maintenance.domain.dto.PmEntryResponse
import com.ampairs.cb_maintenance.domain.dto.applyRequest
import com.ampairs.cb_maintenance.domain.dto.asPmEntryResponse
import com.ampairs.cb_maintenance.domain.model.ChecklistItemResult
import com.ampairs.cb_maintenance.domain.model.FrequencyUnit
import com.ampairs.cb_maintenance.domain.model.PmEntry
import com.ampairs.cb_maintenance.domain.model.PmEntrySource
import com.ampairs.cb_maintenance.domain.model.PmEntryStatus
import com.ampairs.cb_maintenance.domain.model.PmSchedule
import com.ampairs.cb_maintenance.repository.PmEntryRepository
import com.ampairs.cb_maintenance.repository.PmScheduleRepository
import com.ampairs.cb_store.service.StoreService
import com.ampairs.core.sync.EntityChangePublisher
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneOffset

@Service
class PmEntryService(
    private val pmEntryRepository: PmEntryRepository,
    private val pmScheduleRepository: PmScheduleRepository,
    private val storeService: StoreService,
    private val employeeService: EmployeeService,
    private val ticketService: TicketService,
    private val accessService: MaintenanceAccessService,
    private val entityChangePublisher: EntityChangePublisher,
) {
    private val logger = LoggerFactory.getLogger(PmEntryService::class.java)

    @Transactional(readOnly = true)
    fun findEntity(uid: String): PmEntry =
        pmEntryRepository.findByUid(uid) ?: throw MaintenanceNotFoundException("PM entry not found for uid: $uid")

    @Transactional(readOnly = true)
    fun getForCaller(uid: String, caller: EmployeeResponse): PmEntryResponse {
        val entry = findEntity(uid)
        accessService.assertZoneAccess(caller, entry.zonalOfficeId, entry.assignedToEmployeeId)
        return entry.asPmEntryResponse()
    }

    @Transactional(readOnly = true)
    fun getAfterSync(lastSync: String?, zoneFilter: String?, pageable: Pageable): Page<PmEntryResponse> {
        val lastSyncInstant = lastSync?.takeIf { it.isNotBlank() }?.let {
            runCatching { Instant.parse(URLDecoder.decode(it, StandardCharsets.UTF_8)) }
                .onFailure { e -> logger.warn("Invalid last_sync '{}', full feed", lastSync, e) }
                .getOrNull()
        }
        val page: Page<PmEntry> = when {
            zoneFilter == null && lastSyncInstant == null -> pmEntryRepository.findAllForSync(pageable)
            zoneFilter == null -> pmEntryRepository.findByUpdatedAtAfter(lastSyncInstant!!, pageable)
            lastSyncInstant == null -> pmEntryRepository.findByZonalOfficeIdForSync(zoneFilter, pageable)
            else -> pmEntryRepository.findByZonalOfficeIdAndUpdatedAtAfter(zoneFilter, lastSyncInstant, pageable)
        }
        return page.map { it.asPmEntryResponse() }
    }

    @Transactional
    fun bulkUpsert(requests: List<PmEntryRequest>): List<PmEntryResponse> =
        requests.map { request ->
            val existing = request.uid?.takeIf { it.isNotBlank() }?.let { pmEntryRepository.findByUid(it) }
            val entry = (existing ?: PmEntry()).applyRequest(request)
            if (entry.zonalOfficeId.isBlank() && entry.storeId.isNotBlank()) {
                entry.zonalOfficeId = storeService.getZonalOfficeId(entry.storeId)
            }
            // If the store carries no zone, a claimed entry would fall out of the claimer's zone feed
            // (and only stay in the unassigned-orphan branch until assigned). Resolve the orphan into
            // the assignee's zone so a self-assigned PM stays visible to the employee who took it.
            if (entry.zonalOfficeId.isBlank()) {
                entry.assignedToEmployeeId?.takeIf { it.isNotBlank() }?.let { assignee ->
                    runCatching { employeeService.getByUid(assignee).zonalOfficeId }
                        .getOrNull()?.takeIf { it.isNotBlank() }
                        ?.let { entry.zonalOfficeId = it }
                }
            }
            val saved = pmEntryRepository.save(entry)
            // A completion synced up from a device spawns tickets for failures and auto-resolves the
            // ticket this entry was created to address (both idempotent, server-side).
            if (saved.status == PmEntryStatus.DONE) onEntryDone(saved)
            entityChangePublisher.updated("cb_pm_entry", saved.uid)
            saved.asPmEntryResponse()
        }

    /** Complete a PM: auto-assign on action, record results, spawn tickets for failed checks (§4.3, §6). */
    @Transactional
    fun complete(uid: String, checklistResult: List<ChecklistItemResult>?, caller: EmployeeResponse): PmEntryResponse {
        val entry = findEntity(uid)
        accessService.assertZoneAccess(caller, entry.zonalOfficeId, entry.assignedToEmployeeId)
        if (entry.assignedToEmployeeId.isNullOrBlank()) entry.assignedToEmployeeId = caller.uid // autoAssignOnAction
        entry.status = PmEntryStatus.DONE
        entry.completedAt = Instant.now()
        entry.completedByEmployeeId = caller.uid
        if (checklistResult != null) entry.checklistResult = checklistResult
        val saved = pmEntryRepository.save(entry)
        onEntryDone(saved)
        entityChangePublisher.updated("cb_pm_entry", saved.uid)
        return saved.asPmEntryResponse()
    }

    @Transactional
    fun assist(uid: String, caller: EmployeeResponse): PmEntryResponse {
        val entry = findEntity(uid)
        accessService.assertZoneAccess(caller, entry.zonalOfficeId, entry.assignedToEmployeeId)
        val current = entry.assistedByEmployeeIds ?: emptyList()
        if (caller.uid !in current) entry.assistedByEmployeeIds = current + caller.uid
        return pmEntryRepository.save(entry)
            .also { entityChangePublisher.updated("cb_pm_entry", it.uid) }
            .asPmEntryResponse()
    }

    @Transactional
    fun reassign(uid: String, newAssigneeId: String, caller: EmployeeResponse): PmEntryResponse {
        val entry = findEntity(uid)
        accessService.assertZoneAccess(caller, entry.zonalOfficeId, entry.assignedToEmployeeId)
        entry.assignedToEmployeeId = newAssigneeId
        return pmEntryRepository.save(entry)
            .also { entityChangePublisher.updated("cb_pm_entry", it.uid) }
            .asPmEntryResponse()
    }

    /**
     * Nightly-style PM generation for the CURRENT workspace: one next-due entry per
     * (active store, active schedule) pair whose next occurrence falls within the window. The
     * previous entry IS the cursor — no separate bookkeeping table (module plan §3).
     */
    @Transactional
    fun generateDueEntries(windowDays: Int): Int {
        val now = Instant.now()
        val windowEnd = now.plus(java.time.Duration.ofDays(windowDays.toLong()))
        val stores = storeService.findAllActive()
        val schedules = pmScheduleRepository.findByActiveTrue()
        var created = 0
        for (store in stores) {
            for (schedule in schedules) {
                val last = pmEntryRepository
                    .findTopByStoreIdAndPmScheduleIdOrderByDueDateDesc(store.uid, schedule.uid)
                val nextDue = last?.dueDate?.let { addFrequency(it, schedule) } ?: (store.createdAt ?: now)
                if (nextDue.isAfter(windowEnd)) continue
                if (pmEntryRepository.existsByStoreIdAndPmScheduleIdAndDueDate(store.uid, schedule.uid, nextDue)) continue
                val entry = PmEntry().apply {
                    storeId = store.uid
                    zonalOfficeId = store.zonalOfficeId
                    assetCategory = schedule.assetCategory
                    pmScheduleId = schedule.uid
                    source = PmEntrySource.SCHEDULED
                    dueDate = nextDue
                    status = PmEntryStatus.DUE
                }
                pmEntryRepository.save(entry)
                created++
            }
        }
        if (created > 0) logger.info("PM generation created {} entries (window {} days)", created, windowDays)
        return created
    }

    /**
     * Runs when a PM entry reaches DONE: spawn tickets for failed checklist items, then auto-resolve
     * the ticket the entry was created to address (if any). Order matters — spawn first so it can set
     * `entry.ticketId` for a failure-spawned ticket, and the resolve step is guarded against resolving
     * a ticket this same entry just spawned.
     */
    private fun onEntryDone(entry: PmEntry) {
        spawnTicketsForFailures(entry)
        entry.ticketId?.takeIf { it.isNotBlank() }?.let { ticketService.markResolvedFromPmEntry(it, entry.uid) }
    }

    private fun spawnTicketsForFailures(entry: PmEntry) {
        val failures = entry.checklistResult?.filter { !it.passed } ?: return
        if (failures.isEmpty()) return
        var firstSpawned: String? = null
        for (failed in failures) {
            val ticket = ticketService.raiseFromPmFailure(
                storeId = entry.storeId,
                zonalOfficeId = entry.zonalOfficeId,
                assetCategory = entry.assetCategory,
                subCategory = failed.item,
                originPmEntryId = entry.uid,
            )
            if (firstSpawned == null) firstSpawned = ticket?.uid
        }
        if (entry.ticketId.isNullOrBlank() && firstSpawned != null) {
            entry.ticketId = firstSpawned
            pmEntryRepository.save(entry)
        }
    }

    private fun addFrequency(base: Instant, schedule: PmSchedule): Instant {
        val z = base.atZone(ZoneOffset.UTC)
        val n = schedule.frequencyInterval.toLong()
        return when (schedule.frequencyUnit) {
            FrequencyUnit.DAY -> z.plusDays(n)
            FrequencyUnit.WEEK -> z.plusWeeks(n)
            FrequencyUnit.MONTH -> z.plusMonths(n)
            FrequencyUnit.YEAR -> z.plusYears(n)
        }.toInstant()
    }
}
