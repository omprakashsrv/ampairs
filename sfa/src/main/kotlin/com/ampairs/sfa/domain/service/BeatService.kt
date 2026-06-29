package com.ampairs.sfa.domain.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.sfa.domain.model.Beat
import com.ampairs.sfa.domain.model.BeatOutlet
import com.ampairs.sfa.repository.BeatOutletRepository
import com.ampairs.sfa.repository.BeatRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Beats + beat-outlet membership over the offline `/sync` contract (UID-keyed upsert,
 * soft-delete-inclusive incremental feed). @TenantId scopes everything to the distributor workspace.
 */
@Service
@Transactional
class BeatService(
    private val beatRepository: BeatRepository,
    private val beatOutletRepository: BeatOutletRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {

    @Transactional(readOnly = true)
    fun getBeatsAfterSync(lastSync: String?, pageable: Pageable): Page<Beat> =
        syncFeed(lastSync, pageable, { beatRepository.findAll(it) }, { i, p -> beatRepository.findByUpdatedAtAfter(i, p) })

    fun bulkUpsertBeats(incoming: List<Beat>): List<Beat> = incoming.map { row ->
        val existing = row.uid.takeIf { it.isNotBlank() }?.let { beatRepository.findByUid(it) }
        if (existing != null) {
            existing.name = row.name
            existing.description = row.description
            existing.repMemberUid = row.repMemberUid
            existing.scheduledDays = row.scheduledDays
            existing.active = row.active
            beatRepository.save(existing).also { entityChangePublisher.updated("beat", it.uid) }
        } else {
            beatRepository.save(row).also { entityChangePublisher.created("beat", it.uid) }
        }
    }

    @Transactional(readOnly = true)
    fun getBeatOutletsAfterSync(lastSync: String?, pageable: Pageable): Page<BeatOutlet> =
        syncFeed(lastSync, pageable, { beatOutletRepository.findAll(it) }, { i, p -> beatOutletRepository.findByUpdatedAtAfter(i, p) })

    fun bulkUpsertBeatOutlets(incoming: List<BeatOutlet>): List<BeatOutlet> = incoming.map { row ->
        val existing = row.uid.takeIf { it.isNotBlank() }?.let { beatOutletRepository.findByUid(it) }
        if (existing != null) {
            existing.beatUid = row.beatUid
            existing.customerUid = row.customerUid
            existing.visitSequence = row.visitSequence
            existing.visitDay = row.visitDay
            existing.active = row.active
            beatOutletRepository.save(existing).also { entityChangePublisher.updated("beat_outlet", it.uid) }
        } else {
            beatOutletRepository.save(row).also { entityChangePublisher.created("beat_outlet", it.uid) }
        }
    }

    /** Ordered active outlets on a beat — the rep's stop sequence for a route. */
    @Transactional(readOnly = true)
    fun outletsForBeat(beatUid: String): List<BeatOutlet> =
        beatOutletRepository.findByBeatUidAndActiveTrueOrderByVisitSequenceAsc(beatUid)
}
