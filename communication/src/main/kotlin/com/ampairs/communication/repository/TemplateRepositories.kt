package com.ampairs.communication.repository

import com.ampairs.communication.domain.model.MessageTemplate
import com.ampairs.communication.domain.model.TemplateVariant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface MessageTemplateRepository : CrudRepository<MessageTemplate, Long> {
    fun findByUid(uid: String?): MessageTemplate?
    fun findByCode(code: String): MessageTemplate?

    /** Sync feed — INCLUDES inactive rows so deletions propagate. @TenantId filters by workspace. */
    @Query("SELECT t FROM message_template t WHERE t.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(@Param("lastSync") lastSync: Instant, pageable: Pageable): Page<MessageTemplate>

    @Query("SELECT t FROM message_template t")
    fun findAllForSync(pageable: Pageable): Page<MessageTemplate>
}

interface TemplateVariantRepository : CrudRepository<TemplateVariant, Long> {
    fun findByUid(uid: String?): TemplateVariant?
    fun findByTemplateUid(templateUid: String): List<TemplateVariant>
    fun findByTemplateUidIn(templateUids: Collection<String>): List<TemplateVariant>
}
