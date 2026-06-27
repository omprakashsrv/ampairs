package com.ampairs.communication.service

import com.ampairs.communication.domain.model.CommunicationConfig
import com.ampairs.communication.repository.CommunicationConfigRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Lazily provides the single per-workspace [CommunicationConfig], creating defaults on first use. */
@Service
class CommunicationConfigService(
    private val repository: CommunicationConfigRepository,
) {
    @Transactional
    fun getOrCreate(): CommunicationConfig {
        return repository.findForWorkspace().firstOrNull()
            ?: repository.save(CommunicationConfig())
    }
}
