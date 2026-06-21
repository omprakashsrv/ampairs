package com.ampairs.printing.service

import com.ampairs.printing.domain.dto.PrintTemplateRequest
import com.ampairs.printing.domain.dto.PrintTemplateResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PrintTemplateService {

    /** Incremental sync feed (includes soft-deleted rows). Falls back to full feed when lastSync is blank. */
    fun getTemplatesAfterSync(lastSync: String?, pageable: Pageable): Page<PrintTemplateResponse>

    /** Bulk upsert keyed by uid (create if absent, update if present); accepts inactive rows in-band. */
    fun bulkUpsert(requests: List<PrintTemplateRequest>): List<PrintTemplateResponse>

    fun findByUid(uid: String): PrintTemplateResponse?
}
