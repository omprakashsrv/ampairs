package com.ampairs.sfa.repository

import com.ampairs.sfa.domain.model.Attendance
import com.ampairs.sfa.domain.model.Beat
import com.ampairs.sfa.domain.model.BeatOutlet
import com.ampairs.sfa.domain.model.FieldOrder
import com.ampairs.sfa.domain.model.JourneyPlan
import com.ampairs.sfa.domain.model.PlannedVisit
import com.ampairs.sfa.domain.model.Visit
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

/*
 * All SFA repositories follow the offline-sync contract: a max-checkpoint query and an
 * incremental feed that INCLUDES soft-deleted (active = false) rows so deletions propagate.
 * @TenantId on the entities auto-scopes every query to the current workspace.
 */

@Repository
interface BeatRepository : JpaRepository<Beat, Long> {
    fun findByUid(uid: String): Beat?
    fun existsByUid(uid: String): Boolean

    @Query("SELECT MAX(b.updatedAt) FROM Beat b")
    fun findMaxUpdatedAt(): Instant?

    @Query("SELECT b FROM Beat b WHERE b.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(lastSync: Instant, pageable: Pageable): Page<Beat>
}

@Repository
interface BeatOutletRepository : JpaRepository<BeatOutlet, Long> {
    fun findByUid(uid: String): BeatOutlet?
    fun existsByUid(uid: String): Boolean
    fun findByBeatUidAndActiveTrueOrderByVisitSequenceAsc(beatUid: String): List<BeatOutlet>

    @Query("SELECT MAX(o.updatedAt) FROM BeatOutlet o")
    fun findMaxUpdatedAt(): Instant?

    @Query("SELECT o FROM BeatOutlet o WHERE o.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(lastSync: Instant, pageable: Pageable): Page<BeatOutlet>
}

@Repository
interface JourneyPlanRepository : JpaRepository<JourneyPlan, Long> {
    fun findByUid(uid: String): JourneyPlan?
    fun existsByUid(uid: String): Boolean

    @Query("SELECT MAX(j.updatedAt) FROM JourneyPlan j")
    fun findMaxUpdatedAt(): Instant?

    @Query("SELECT j FROM JourneyPlan j WHERE j.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(lastSync: Instant, pageable: Pageable): Page<JourneyPlan>
}

@Repository
interface PlannedVisitRepository : JpaRepository<PlannedVisit, Long> {
    fun findByUid(uid: String): PlannedVisit?
    fun existsByUid(uid: String): Boolean
    fun findByRepMemberUidAndPlannedDateBetween(repMemberUid: String, from: Instant, to: Instant): List<PlannedVisit>

    @Query("SELECT MAX(p.updatedAt) FROM PlannedVisit p")
    fun findMaxUpdatedAt(): Instant?

    @Query("SELECT p FROM PlannedVisit p WHERE p.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(lastSync: Instant, pageable: Pageable): Page<PlannedVisit>
}

@Repository
interface VisitRepository : JpaRepository<Visit, Long> {
    fun findByUid(uid: String): Visit?
    fun existsByUid(uid: String): Boolean
    fun findByRepMemberUidAndVisitedAtBetween(repMemberUid: String, from: Instant, to: Instant): List<Visit>

    @Query("SELECT MAX(v.updatedAt) FROM Visit v")
    fun findMaxUpdatedAt(): Instant?

    @Query("SELECT v FROM Visit v WHERE v.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(lastSync: Instant, pageable: Pageable): Page<Visit>
}

@Repository
interface AttendanceRepository : JpaRepository<Attendance, Long> {
    fun findByUid(uid: String): Attendance?
    fun existsByUid(uid: String): Boolean

    @Query("SELECT MAX(a.updatedAt) FROM Attendance a")
    fun findMaxUpdatedAt(): Instant?

    @Query("SELECT a FROM Attendance a WHERE a.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(lastSync: Instant, pageable: Pageable): Page<Attendance>
}

@Repository
interface FieldOrderRepository : JpaRepository<FieldOrder, Long> {
    fun findByUid(uid: String): FieldOrder?
    fun existsByUid(uid: String): Boolean

    @Query("SELECT MAX(f.updatedAt) FROM FieldOrder f")
    fun findMaxUpdatedAt(): Instant?

    @Query("SELECT f FROM FieldOrder f WHERE f.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(lastSync: Instant, pageable: Pageable): Page<FieldOrder>
}
