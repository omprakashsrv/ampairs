package com.ampairs.workspace.service

import com.ampairs.workspace.model.dto.LlmModelResponse
import com.ampairs.workspace.model.dto.asLlmModelResponse
import com.ampairs.workspace.repository.MasterLlmModelRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read access to the server-seeded LLM model catalog for mobile clients.
 */
@Service
class WorkspaceLlmModelService(
    private val masterLlmModelRepository: MasterLlmModelRepository,
) {

    @Transactional(readOnly = true)
    fun getCatalog(): List<LlmModelResponse> =
        masterLlmModelRepository.findByActiveTrueOrderByDisplayOrderAsc()
            .map { it.asLlmModelResponse() }
}
