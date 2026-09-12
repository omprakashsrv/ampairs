package com.ampairs.cb_maintenance.repository

import com.ampairs.cb_maintenance.domain.model.TicketBucket
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface TicketBucketRepository : CrudRepository<TicketBucket, Long> {

    fun findByUid(uid: String?): TicketBucket?

    @Query("SELECT MAX(t.updatedAt) FROM cb_ticket_bucket t")
    fun findMaxUpdatedAt(): Instant?

    @EntityGraph("CbTicketBucket.basic")
    @Query("SELECT t FROM cb_ticket_bucket t")
    fun findAllForSync(pageable: Pageable): Page<TicketBucket>

    @EntityGraph("CbTicketBucket.basic")
    @Query("SELECT t FROM cb_ticket_bucket t WHERE t.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(@Param("lastSync") lastSync: Instant, pageable: Pageable): Page<TicketBucket>
}
