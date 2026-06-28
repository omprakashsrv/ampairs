package com.ampairs.communication.service.schedule

import com.ampairs.communication.domain.enums.Channel
import com.ampairs.communication.domain.enums.Frequency
import com.ampairs.communication.domain.enums.TriggerType
import com.ampairs.communication.domain.model.CommunicationOccurrence
import com.ampairs.communication.domain.model.CommunicationSchedule
import com.ampairs.communication.port.CustomerAudiencePort
import com.ampairs.communication.repository.CommunicationOccurrenceRepository
import com.ampairs.communication.repository.CommunicationScheduleRepository
import com.ampairs.communication.service.send.CommunicationDispatchService
import com.ampairs.communication.service.template.TemplateService
import com.ampairs.core.multitenancy.TenantContextHolder
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Materializes due recurring schedules into sends. At-most-once is guaranteed by the unique
 * `(schedule_uid, occurrence_key)` ledger row — even if two instances run concurrently, the second
 * insert fails and that occurrence is skipped. After firing, advances `nextRunAt` in the workspace
 * business timezone (FR-018/019/020/021/022).
 */
@Component
class ScheduleSweeper(
    private val scheduleRepository: CommunicationScheduleRepository,
    private val occurrenceRepository: CommunicationOccurrenceRepository,
    private val templateService: TemplateService,
    private val audiencePort: CustomerAudiencePort,
    private val dispatchService: CommunicationDispatchService,
    private val recurrenceCalculator: RecurrenceCalculator,
) {
    private val logger = LoggerFactory.getLogger(ScheduleSweeper::class.java)
    private val objectMapper = ObjectMapper()

    @Scheduled(
        fixedDelayString = "\${communication.scheduler.tick-seconds:60}000",
        initialDelayString = "\${communication.scheduler.tick-seconds:60}000",
    )
    fun sweep() {
        val now = Instant.now()
        val due = runCatching { scheduleRepository.findDue(now) }.getOrElse {
            logger.warn("Schedule sweep query failed: {}", it.message); return
        }
        if (due.isEmpty()) return
        logger.info("Schedule sweep: {} due", due.size)
        due.forEach { schedule -> runInTenant(schedule.ownerId) { fire(schedule, now) } }
    }

    @Transactional
    fun fire(schedule: CommunicationSchedule, now: Instant) {
        val dueAt = schedule.nextRunAt ?: return
        val occurrenceKey = recurrenceCalculator.occurrenceKey(dueAt, schedule.timezone)

        // At-most-once guard: claim the occurrence first.
        if (occurrenceRepository.existsByScheduleUidAndOccurrenceKey(schedule.uid, occurrenceKey)) {
            advance(schedule, dueAt, occurrenceKey, now)
            return
        }
        try {
            occurrenceRepository.save(CommunicationOccurrence().apply {
                scheduleUid = schedule.uid; this.occurrenceKey = occurrenceKey; ownerId = schedule.ownerId
            })
        } catch (e: DataIntegrityViolationException) {
            logger.debug("Occurrence {}:{} already claimed; skipping", schedule.uid, occurrenceKey)
            advance(schedule, dueAt, occurrenceKey, now)
            return
        }

        materialize(schedule, occurrenceKey)
        advance(schedule, dueAt, occurrenceKey, now)
    }

    private fun materialize(schedule: CommunicationSchedule, occurrenceKey: String) {
        val (template, variants) = templateService.findByUid(schedule.templateUid) ?: run {
            logger.warn("Schedule {} references missing template {}", schedule.uid, schedule.templateUid)
            return
        }
        val channels = schedule.channels.split(",")
            .mapNotNull { runCatching { Channel.valueOf(it.trim().uppercase()) }.getOrNull() }
        val recipients = audiencePort.resolve(schedule.audienceType, schedule.audienceRef, explicit = emptyList())
        if (recipients.isEmpty()) {
            logger.warn("Schedule {} resolved no recipients for audience {}", schedule.uid, schedule.audienceRef)
            return
        }
        dispatchService.dispatch(
            template = template,
            variants = variants,
            channels = channels,
            recipients = recipients,
            variables = parseVariables(schedule.variablesJson),
            triggerType = TriggerType.SCHEDULE,
            sourceRef = schedule.uid,
            dedupKey = "${schedule.uid}:$occurrenceKey",
        )
    }

    private fun advance(schedule: CommunicationSchedule, dueAt: Instant, occurrenceKey: String, now: Instant) {
        val frequency = runCatching { Frequency.valueOf(schedule.frequency.uppercase()) }.getOrNull() ?: return
        val nextRun = recurrenceCalculator.next(
            frequency = frequency,
            interval = schedule.interval,
            dayOfWeek = schedule.dayOfWeek,
            dayOfMonth = schedule.dayOfMonth,
            timeOfDay = schedule.timeOfDay,
            timezone = schedule.timezone,
            after = dueAt,
            startDate = schedule.startDate,
            endDate = schedule.endDate,
        )
        schedule.lastRunAt = now
        schedule.lastOccurrenceKey = occurrenceKey
        schedule.nextRunAt = nextRun // null → no further occurrences; sweeper won't pick it up again
        schedule.claimVersion += 1
        scheduleRepository.save(schedule)
    }

    private fun parseVariables(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            objectMapper.readValue(json, object : TypeReference<Map<String, String>>() {})
        }.getOrDefault(emptyMap())
    }

    private inline fun runInTenant(tenantId: String, block: () -> Unit) {
        val prior = TenantContextHolder.getCurrentTenant()
        TenantContextHolder.setCurrentTenant(tenantId)
        try {
            block()
        } catch (e: Exception) {
            logger.error("Schedule fire failed for tenant {}: {}", tenantId, e.message, e)
        } finally {
            if (prior != null) TenantContextHolder.setCurrentTenant(prior) else TenantContextHolder.clearTenantContext()
        }
    }
}
