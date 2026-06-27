package com.ampairs.communication.repository

import com.ampairs.communication.domain.model.CommunicationConfig
import com.ampairs.communication.domain.model.EventTemplateBinding
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface EventTemplateBindingRepository : CrudRepository<EventTemplateBinding, Long> {
    fun findByUid(uid: String?): EventTemplateBinding?
    fun findByEventTypeAndEnabledTrueAndActiveTrue(eventType: String): EventTemplateBinding?

    @Query("SELECT b FROM event_template_binding b WHERE b.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(@Param("lastSync") lastSync: Instant, pageable: Pageable): Page<EventTemplateBinding>

    @Query("SELECT b FROM event_template_binding b")
    fun findAllForSync(pageable: Pageable): Page<EventTemplateBinding>
}

interface CommunicationConfigRepository : CrudRepository<CommunicationConfig, Long> {
    fun findByUid(uid: String?): CommunicationConfig?

    /** One row per workspace; @TenantId filters by the active tenant. */
    @Query("SELECT c FROM communication_config c")
    fun findForWorkspace(): List<CommunicationConfig>
}
