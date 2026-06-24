package com.ampairs.workspace.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.workspace.model.enums.LlmModelRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * Server-seeded catalog of on-device LLM models the mobile assistant can download.
 *
 * This is the single source of truth for model metadata (download URLs, sizes, sampling params).
 * The mobile app pulls the active rows via `GET /workspace/v1/llm-models/catalog` and selects one
 * by device RAM + role locally — exactly like the module catalog. Global (not tenant-scoped):
 * the same model set is offered to every workspace.
 *
 * Seeded/updated idempotently on startup by [com.ampairs.workspace.service.MasterLlmModelSeederService].
 */
@Entity
@Table(
    name = "master_llm_models",
    indexes = [
        Index(name = "idx_master_llm_model_id", columnList = "model_id", unique = true),
        Index(name = "idx_master_llm_model_role", columnList = "role"),
        Index(name = "idx_master_llm_model_active", columnList = "active"),
    ],
)
class MasterLlmModel : BaseDomain() {

    /** Stable business id, e.g. "gemma-3n-e2b" (matches the app's ModelDescriptor.id). */
    @Column(name = "model_id", nullable = false, unique = true, length = 100)
    var modelId: String = ""

    @Column(name = "display_name", nullable = false, length = 200)
    var displayName: String = ""

    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var role: LlmModelRole = LlmModelRole.CHAT

    /** Which engine runs this model, e.g. "litert-lm" or "llamacpp". */
    @Column(name = "backend_id", nullable = false, length = 50)
    var backendId: String = ""

    @Column(name = "file_name", nullable = false, length = 255)
    var fileName: String = ""

    @Column(name = "download_url", nullable = false, length = 1000)
    var downloadUrl: String = ""

    @Column(name = "size_bytes", nullable = false)
    var sizeBytes: Long = 0

    @Column(name = "estimated_peak_memory_bytes", nullable = false)
    var estimatedPeakMemoryBytes: Long = 0

    /** Optional lowercase-hex SHA-256; when set, the app verifies the file after download. */
    @Column(name = "sha256", length = 64)
    var sha256: String? = null

    @Column(name = "temperature", nullable = false)
    var temperature: Double = 0.0

    @Column(name = "top_k", nullable = false)
    var topK: Int = 1

    @Column(name = "top_p", nullable = false)
    var topP: Double = 1.0

    @Column(name = "max_tokens", nullable = false)
    var maxTokens: Int = 512

    /** Display/selection order within a role. */
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    /** Whether this model is offered to clients. Inactive rows are excluded from the catalog. */
    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = "LLM"
}
