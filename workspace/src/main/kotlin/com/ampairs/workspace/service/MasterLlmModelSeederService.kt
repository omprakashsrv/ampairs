package com.ampairs.workspace.service

import com.ampairs.workspace.model.MasterLlmModel
import com.ampairs.workspace.model.enums.LlmModelRole
import com.ampairs.workspace.repository.MasterLlmModelRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Seeds the on-device LLM model catalog on startup. Idempotent: inserts missing models and refreshes
 * metadata (download URL, sizes, params) for existing ones, keyed by [MasterLlmModel.modelId].
 *
 * Values mirror the app's bundled `ModelCatalog` (the offline fallback). The download URLs are
 * provisional placeholders pending model-hosting finalization — update them here (single source of
 * truth); clients pick them up on the next catalog pull.
 */
@Service
@Transactional
class MasterLlmModelSeederService(
    private val masterLlmModelRepository: MasterLlmModelRepository,
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(MasterLlmModelSeederService::class.java)

    override fun run(vararg args: String) {
        seedModels()
    }

    fun seedModels() {
        logger.info("Starting LLM model catalog seeding...")
        val existing = masterLlmModelRepository.findAll().associateBy { it.modelId }

        systemModels().forEach { incoming ->
            val current = existing[incoming.modelId]
            if (current == null) {
                logger.info("Seeding new LLM model: {}", incoming.modelId)
                masterLlmModelRepository.save(incoming)
            } else {
                current.apply {
                    displayName = incoming.displayName
                    role = incoming.role
                    backendId = incoming.backendId
                    fileName = incoming.fileName
                    downloadUrl = incoming.downloadUrl
                    sizeBytes = incoming.sizeBytes
                    estimatedPeakMemoryBytes = incoming.estimatedPeakMemoryBytes
                    sha256 = incoming.sha256
                    temperature = incoming.temperature
                    topK = incoming.topK
                    topP = incoming.topP
                    maxTokens = incoming.maxTokens
                    displayOrder = incoming.displayOrder
                    active = incoming.active
                }
                masterLlmModelRepository.save(current)
            }
        }
        logger.info("LLM model catalog seeding completed")
    }

    private fun systemModels(): List<MasterLlmModel> = listOf(
        // Intent → action tool-calling (default). Tiny + fast; runs on low-end devices.
        MasterLlmModel().apply {
            modelId = "function-gemma-270m"
            displayName = "FunctionGemma 270M"
            role = LlmModelRole.INTENT
            backendId = "litert-lm"
            fileName = "function-gemma-270m.litertlm"
            downloadUrl = "https://huggingface.co/google/function-gemma-270m"
            sizeBytes = 300_000_000L
            estimatedPeakMemoryBytes = 700_000_000L
            temperature = 0.0
            topK = 1
            topP = 1.0
            maxTokens = 256
            displayOrder = 10
            active = true
        },
        // Conversational answers — low-RAM tier (3–6 GB).
        MasterLlmModel().apply {
            modelId = "gemma-3n-e2b"
            displayName = "Gemma 3n E2B"
            role = LlmModelRole.CHAT
            backendId = "litert-lm"
            fileName = "gemma-3n-e2b.litertlm"
            downloadUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm"
            sizeBytes = 3_000_000_000L
            estimatedPeakMemoryBytes = 3_500_000_000L
            temperature = 0.3
            topK = 40
            topP = 0.95
            maxTokens = 512
            displayOrder = 20
            active = true
        },
        // Conversational answers — higher-RAM tier (≥ 6 GB).
        MasterLlmModel().apply {
            modelId = "gemma-3n-e4b"
            displayName = "Gemma 3n E4B"
            role = LlmModelRole.CHAT
            backendId = "litert-lm"
            fileName = "gemma-3n-e4b.litertlm"
            downloadUrl = "https://huggingface.co/google/gemma-3n-E4B-it-litert-lm"
            sizeBytes = 4_400_000_000L
            estimatedPeakMemoryBytes = 6_000_000_000L
            temperature = 0.3
            topK = 40
            topP = 0.95
            maxTokens = 512
            displayOrder = 30
            active = true
        },
        // llama.cpp fallback / parity (Desktop, or when LiteRT-LM is unavailable). Needs ≥ 6 GB.
        MasterLlmModel().apply {
            modelId = "qwen2.5-3b-instruct"
            displayName = "Qwen2.5 3B Instruct (GGUF)"
            role = LlmModelRole.FALLBACK
            backendId = "llamacpp"
            fileName = "qwen2.5-3b-instruct-q4_k_m.gguf"
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF"
            sizeBytes = 2_000_000_000L
            estimatedPeakMemoryBytes = 4_000_000_000L
            temperature = 0.2
            topK = 40
            topP = 0.9
            maxTokens = 512
            displayOrder = 40
            active = true
        },
    )
}
