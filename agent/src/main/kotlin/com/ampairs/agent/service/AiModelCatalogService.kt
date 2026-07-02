package com.ampairs.agent.service

import com.ampairs.agent.domain.model.AiModelCatalog
import com.ampairs.agent.domain.model.AiModelDescriptor
import org.springframework.stereotype.Service

/**
 * Read-only access to the curated [AiModelCatalog]. No tenant scoping — the catalog is global
 * reference data shared by every workspace.
 */
@Service
class AiModelCatalogService {

    fun listModels(): List<AiModelDescriptor> = AiModelCatalog.MODELS

    fun findById(id: String): AiModelDescriptor? = AiModelCatalog.byId(id)
}
