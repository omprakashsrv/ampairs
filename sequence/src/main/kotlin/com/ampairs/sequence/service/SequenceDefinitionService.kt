package com.ampairs.sequence.service

import com.ampairs.sequence.domain.dto.SequenceDefinitionRequest
import com.ampairs.sequence.domain.dto.SequenceDefinitionResponse
import com.ampairs.sequence.domain.dto.SequenceNumberResponse
import com.ampairs.sequence.domain.dto.SequencePreviewResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface SequenceDefinitionService {

    fun findAll(): List<SequenceDefinitionResponse>

    fun findByUid(uid: String): SequenceDefinitionResponse?

    /** Incremental sync feed — includes inactive rows so clients detect deactivations. */
    fun getDefinitionsAfterSync(lastSync: String?, pageable: Pageable): Page<SequenceDefinitionResponse>

    /**
     * UID-keyed bulk upsert (canonical sync push). The server counter is authoritative:
     * a client current_value lower than the server's is ignored, never an error.
     */
    fun bulkUpsert(requests: List<SequenceDefinitionRequest>): List<SequenceDefinitionResponse>

    fun create(request: SequenceDefinitionRequest): SequenceDefinitionResponse

    fun update(uid: String, request: SequenceDefinitionRequest): SequenceDefinitionResponse

    /** Next number a definition would issue — nothing is advanced or reserved (FR-006). */
    fun preview(uid: String): SequencePreviewResponse

    /** Direct server-side generation (FR-015): resolve, atomically advance, format. */
    fun next(entityType: String, userId: String?): SequenceNumberResponse
}
