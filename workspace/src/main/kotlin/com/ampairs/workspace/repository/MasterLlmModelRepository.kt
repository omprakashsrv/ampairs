package com.ampairs.workspace.repository

import com.ampairs.workspace.model.MasterLlmModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository for the server-seeded LLM model catalog.
 */
@Repository
interface MasterLlmModelRepository : JpaRepository<MasterLlmModel, String> {

    fun findByModelId(modelId: String): MasterLlmModel?

    fun findByActiveTrueOrderByDisplayOrderAsc(): List<MasterLlmModel>
}
