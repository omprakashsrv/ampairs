package com.ampairs.cb_maintenance.service

import com.ampairs.cb_employee.domain.dto.EmployeeResponse
import com.ampairs.cb_maintenance.domain.dto.TicketRequest
import com.ampairs.cb_maintenance.domain.dto.TicketResponse
import com.ampairs.cb_maintenance.domain.dto.applyRequest
import com.ampairs.cb_maintenance.domain.dto.asTicketResponse
import com.ampairs.cb_maintenance.domain.model.Ticket
import com.ampairs.cb_maintenance.domain.model.TicketStatus
import com.ampairs.cb_maintenance.repository.TicketRepository
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

@Service
class TicketService(
    private val ticketRepository: TicketRepository,
    private val storeService: StoreService,
    private val assignmentService: AssignmentService,
    private val accessService: MaintenanceAccessService,
    private val entityChangePublisher: EntityChangePublisher,
) {
    private val logger = LoggerFactory.getLogger(TicketService::class.java)

    @Transactional(readOnly = true)
    fun findByUid(uid: String): Ticket =
        ticketRepository.findByUid(uid) ?: throw MaintenanceNotFoundException("Ticket not found for uid: $uid")

    /** Single-record fetch with a zone-access check (module plan §5). */
    @Transactional(readOnly = true)
    fun getForCaller(uid: String, caller: EmployeeResponse): TicketResponse {
        val ticket = findByUid(uid)
        accessService.assertZoneAccess(caller, ticket.zonalOfficeId, ticket.assignedToEmployeeId)
        return ticket.asTicketResponse()
    }

    @Transactional(readOnly = true)
    fun getAfterSync(lastSync: String?, zoneFilter: String?, pageable: Pageable): Page<TicketResponse> {
        val lastSyncInstant = lastSync?.takeIf { it.isNotBlank() }?.let {
            runCatching { Instant.parse(URLDecoder.decode(it, StandardCharsets.UTF_8)) }
                .onFailure { e -> logger.warn("Invalid last_sync '{}', full feed", lastSync, e) }
                .getOrNull()
        }
        val page: Page<Ticket> = when {
            zoneFilter == null && lastSyncInstant == null -> ticketRepository.findAllForSync(pageable)
            zoneFilter == null -> ticketRepository.findByUpdatedAtAfter(lastSyncInstant!!, pageable)
            lastSyncInstant == null -> ticketRepository.findByZonalOfficeIdForSync(zoneFilter, pageable)
            else -> ticketRepository.findByZonalOfficeIdAndUpdatedAtAfter(zoneFilter, lastSyncInstant, pageable)
        }
        return page.map { it.asTicketResponse() }
    }

    @Transactional
    fun bulkUpsert(requests: List<TicketRequest>): List<TicketResponse> =
        requests.map { request ->
            val existing = request.uid?.takeIf { it.isNotBlank() }?.let { ticketRepository.findByUid(it) }
            val ticket = (existing ?: Ticket()).applyRequest(request)
            if (ticket.zonalOfficeId.isBlank() && ticket.storeId.isNotBlank()) {
                ticket.zonalOfficeId = storeService.getZonalOfficeId(ticket.storeId)
            }
            ticketRepository.save(ticket)
                .also { entityChangePublisher.updated("cb_ticket", it.uid) }
                .asTicketResponse()
        }

    /** Raise a reactive ticket; denormalizes the zone and auto-assigns by load-balancing (§4). */
    @Transactional
    fun raiseTicket(request: TicketRequest, caller: EmployeeResponse): TicketResponse {
        val ticket = Ticket().applyRequest(request)
        ticket.zonalOfficeId = ticket.zonalOfficeId.ifBlank { storeService.getZonalOfficeId(ticket.storeId) }
        if (ticket.raisedByEmployeeId.isNullOrBlank()) ticket.raisedByEmployeeId = caller.uid
        assignmentService.pickAssignee(ticket.zonalOfficeId)?.let {
            ticket.assignedToEmployeeId = it
            ticket.status = TicketStatus.ASSIGNED
        }
        val saved = ticketRepository.save(ticket)
        entityChangePublisher.created("cb_ticket", saved.uid)
        return saved.asTicketResponse()
    }

    /**
     * Spawn a ticket from a failed PM checklist item (§6). Idempotent on (originPmEntryId, subCategory)
     * so a re-synced DONE entry never double-spawns. Returns null when already present.
     */
    @Transactional
    fun raiseFromPmFailure(
        storeId: String,
        zonalOfficeId: String,
        assetCategory: String,
        subCategory: String,
        originPmEntryId: String,
    ): Ticket? {
        if (ticketRepository.existsByOriginPmEntryIdAndSubCategory(originPmEntryId, subCategory)) return null
        val ticket = Ticket().apply {
            this.storeId = storeId
            this.zonalOfficeId = zonalOfficeId
            this.assetCategory = assetCategory
            this.subCategory = subCategory
            this.originPmEntryId = originPmEntryId
            this.status = TicketStatus.OPEN
        }
        assignmentService.pickAssignee(zonalOfficeId)?.let {
            ticket.assignedToEmployeeId = it
            ticket.status = TicketStatus.ASSIGNED
        }
        return ticketRepository.save(ticket).also { entityChangePublisher.created("cb_ticket", it.uid) }
    }

    /**
     * Auto-resolve the ticket a completed PM entry was created to address (ticket → PM-task → done).
     * Idempotent and guarded: never resolves a ticket that this very entry SPAWNED from a failed check
     * (that is the opposite PM-failure → ticket direction), and never re-resolves a terminal ticket.
     */
    @Transactional
    fun markResolvedFromPmEntry(ticketUid: String, resolvingEntryUid: String) {
        val ticket = ticketRepository.findByUid(ticketUid) ?: return
        if (ticket.originPmEntryId == resolvingEntryUid) return
        if (ticket.status == TicketStatus.RESOLVED || ticket.status == TicketStatus.CLOSED) return
        ticket.status = TicketStatus.RESOLVED
        ticket.resolvedAt = Instant.now()
        ticketRepository.save(ticket)
        entityChangePublisher.updated("cb_ticket", ticket.uid)
    }

    @Transactional
    fun reassign(uid: String, newAssigneeId: String, caller: EmployeeResponse): TicketResponse {
        val ticket = findByUid(uid)
        accessService.assertZoneAccess(caller, ticket.zonalOfficeId, ticket.assignedToEmployeeId)
        if (!assignmentService.isValidAssignee(ticket.zonalOfficeId, newAssigneeId)) {
            throw MaintenanceValidationException("Not a valid assignee for this ticket")
        }
        ticket.assignedToEmployeeId = newAssigneeId
        return ticketRepository.save(ticket)
            .also { entityChangePublisher.updated("cb_ticket", it.uid) }
            .asTicketResponse()
    }

    @Transactional
    fun assist(uid: String, caller: EmployeeResponse): TicketResponse {
        val ticket = findByUid(uid)
        accessService.assertZoneAccess(caller, ticket.zonalOfficeId, ticket.assignedToEmployeeId)
        val current = ticket.assistedByEmployeeIds ?: emptyList()
        if (caller.uid !in current) ticket.assistedByEmployeeIds = current + caller.uid
        return ticketRepository.save(ticket)
            .also { entityChangePublisher.updated("cb_ticket", it.uid) }
            .asTicketResponse()
    }
}
