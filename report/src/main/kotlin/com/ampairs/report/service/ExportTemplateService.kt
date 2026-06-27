package com.ampairs.report.service

import com.ampairs.report.domain.dto.ExportTemplateRequest
import com.ampairs.report.domain.dto.ExportTemplateResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ExportTemplateService {

    fun findByUid(uid: String): ExportTemplateResponse?

    /**
     * Incremental sync feed — templates updated at/after lastSync, INCLUDING inactive
     * (soft-deleted) rows so clients can detect deletions. Blank/null lastSync returns
     * all rows for the workspace (paginated), still including inactive.
     */
    fun getTemplatesAfterSync(lastSync: String?, pageable: Pageable): Page<ExportTemplateResponse>

    /**
     * Bulk upsert templates keyed by uid — create when uid absent/unknown, update when present.
     * Honors the active flag so soft-deletes propagate in-band.
     */
    fun bulkUpsert(requests: List<ExportTemplateRequest>): List<ExportTemplateResponse>
}
