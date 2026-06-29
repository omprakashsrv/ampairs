package com.ampairs.sfa.domain.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.sfa.domain.model.Leave
import com.ampairs.sfa.repository.LeaveRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Manager-marked rep leave. Supports both direct CRUD (manager UI) and the offline `/sync` feed
 * (so excused days reach the rep app).
 */
@Service
@Transactional
class LeaveService(
    private val leaveRepository: LeaveRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {

    fun create(leave: Leave): Leave =
        leaveRepository.save(leave).also { entityChangePublisher.created("leave", it.uid) }

    @Transactional(readOnly = true)
    fun list(repMemberUid: String, from: Instant, to: Instant): List<Leave> =
        leaveRepository.findByRepMemberUidAndLeaveDateBetween(repMemberUid, from, to)

    /** Soft-delete so the deletion propagates over `/sync`. */
    fun delete(uid: String): Boolean {
        val existing = leaveRepository.findByUid(uid) ?: return false
        existing.active = false
        leaveRepository.save(existing)
        entityChangePublisher.updated("leave", existing.uid)
        return true
    }

    @Transactional(readOnly = true)
    fun getLeavesAfterSync(lastSync: String?, pageable: Pageable): Page<Leave> =
        syncFeed(lastSync, pageable, { leaveRepository.findAll(it) }, { i, p -> leaveRepository.findByUpdatedAtAfter(i, p) })

    fun bulkUpsertLeaves(incoming: List<Leave>): List<Leave> = incoming.map { row ->
        val existing = row.uid.takeIf { it.isNotBlank() }?.let { leaveRepository.findByUid(it) }
        if (existing != null) {
            existing.repMemberUid = row.repMemberUid
            existing.leaveDate = row.leaveDate
            existing.reason = row.reason
            existing.markedBy = row.markedBy
            existing.active = row.active
            leaveRepository.save(existing).also { entityChangePublisher.updated("leave", it.uid) }
        } else {
            leaveRepository.save(row).also { entityChangePublisher.created("leave", it.uid) }
        }
    }
}
