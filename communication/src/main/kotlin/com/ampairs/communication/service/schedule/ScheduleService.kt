package com.ampairs.communication.service.schedule

import com.ampairs.communication.domain.dto.ScheduleRequest
import com.ampairs.communication.domain.dto.ScheduleResponse
import com.ampairs.communication.domain.dto.applyRequest
import com.ampairs.communication.domain.dto.asResponse
import com.ampairs.communication.domain.enums.Frequency
import com.ampairs.communication.domain.model.CommunicationSchedule
import com.ampairs.communication.repository.CommunicationScheduleRepository
import com.ampairs.core.sync.EntityChangePublisher
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

/** CRUD + `/sync` for recurring schedules. The server owns `next_run_at` (recomputed on every write). */
@Service
class ScheduleService(
    private val repository: CommunicationScheduleRepository,
    private val recurrenceCalculator: RecurrenceCalculator,
    private val entityChangePublisher: EntityChangePublisher,
) {
    private val logger = LoggerFactory.getLogger(ScheduleService::class.java)

    @Transactional(readOnly = true)
    fun getAfterSync(lastSync: String?, pageable: Pageable): Page<ScheduleResponse> {
        val page: Page<CommunicationSchedule> = if (lastSync.isNullOrBlank()) {
            repository.findAllForSync(pageable)
        } else {
            try {
                repository.findByUpdatedAtAfter(
                    Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8)), pageable
                )
            } catch (e: Exception) {
                logger.warn("Invalid last_sync '{}', full feed", lastSync, e)
                repository.findAllForSync(pageable)
            }
        }
        return page.map { it.asResponse() }
    }

    @Transactional
    fun bulkUpsert(requests: List<ScheduleRequest>): List<ScheduleResponse> = requests.map { req ->
        val schedule = (repository.findByUid(req.uid) ?: CommunicationSchedule()).applyRequest(req)
        schedule.nextRunAt = if (schedule.paused || !schedule.active) null else computeNextRun(schedule)
        repository.save(schedule)
            .also { entityChangePublisher.updated("communication_schedule", it.uid) }
            .asResponse()
    }

    private fun computeNextRun(schedule: CommunicationSchedule): Instant? {
        val frequency = runCatching { Frequency.valueOf(schedule.frequency.uppercase()) }.getOrNull() ?: return null
        return recurrenceCalculator.next(
            frequency = frequency,
            interval = schedule.interval,
            dayOfWeek = schedule.dayOfWeek,
            dayOfMonth = schedule.dayOfMonth,
            timeOfDay = schedule.timeOfDay,
            timezone = schedule.timezone,
            after = Instant.now(),
            startDate = schedule.startDate,
            endDate = schedule.endDate,
        )
    }
}
