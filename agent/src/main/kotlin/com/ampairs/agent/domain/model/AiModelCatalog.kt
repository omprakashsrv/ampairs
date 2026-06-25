package com.ampairs.agent.domain.model

/**
 * Target platform an on-device model can run on. The mobile app filters/disables models whose
 * [AiModelDescriptor.platforms] does not include the running platform.
 */
enum class ModelPlatform { ANDROID, IOS, DESKTOP }

/**
 * The role a model plays in the on-device pipeline. Mirrors the app's `ModelRole` so the manifest can
 * tell the client which entry is the tool-caller vs a conversational model (the app gates auto-pick on
 * this — only [CHAT] models are auto-selected; [INTENT] is the function/tool-calling model).
 *
 *  - [INTENT]   — tiny function/tool-caller (intent → action), e.g. FunctionGemma-270M.
 *  - [CHAT]     — conversational answer model, auto-selectable by the RAM tier.
 *  - [FALLBACK] — alternative chat model the user can pick, not part of the RAM auto-pick.
 */
enum class ModelRole { INTENT, CHAT, FALLBACK }

/**
 * One downloadable on-device LLM (a LiteRT-LM `.litertlm` checkpoint). This is curated reference data,
 * not a tenant entity — it lives in code, not the DB (no per-workspace overrides, changes ship with
 * backend releases).
 *
 * The app downloads the bytes through the backend proxy (`GET /agent/v1/models/{id}/download`), so
 * [sourceUrl] is intentionally **server-side only** and never leaves the backend in the manifest.
 *
 * @property id stable identifier used in the download path and persisted by the app as the selected model.
 * @property role which pipeline slot the app uses this model for (tool-caller vs chat) — see [ModelRole].
 * @property fileName the on-disk filename the app stores and the LiteRT-LM engine loads — must match
 *   exactly what the engine expects (`filesDir/agent_models/{fileName}`).
 * @property sizeBytes approximate download size, for the app's pre-download size warning. The proxy's
 *   `Content-Length` is authoritative for actual progress.
 * @property sha256 lowercase hex digest for post-download integrity verification, or null to skip
 *   verification (TODO: fill in real digests once mirrored on the backend CDN).
 * @property requiredRamMb minimum device total RAM (MB) to run this model; the app disables selection
 *   below this and shows a "needs X / device has Y" message.
 * @property backendId which app-side LLM backend runs it (`litert-lm`).
 * @property sourceUrl upstream URL the backend proxy streams from (HuggingFace litert-community / google).
 */
data class AiModelDescriptor(
    val id: String,
    val name: String,
    val family: String,
    val parameterLabel: String,
    val role: ModelRole,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String?,
    val requiredRamMb: Int,
    val backendId: String,
    val platforms: Set<ModelPlatform>,
    val recommended: Boolean,
    val sourceUrl: String,
)

/**
 * The curated catalog served by `/agent/v1/models`. All models are exposed; the app shows every
 * entry and disables the ones the device can't run (per [AiModelDescriptor.requiredRamMb] /
 * [AiModelDescriptor.platforms]).
 *
 * This list mirrors the Google AI Edge Gallery `model_allowlists` (the canonical LiteRT-LM manifest):
 * `modelId` + `modelFile` + `sizeInBytes` are copied verbatim so the proxy resolves a real file and
 * the app's exact-size check passes. `sizeBytes` MUST match the upstream file byte-for-byte — the app
 * fails the download otherwise.
 *
 * NOTE: [AiModelDescriptor.sha256] is null today (app-side verification skipped until digests are
 * mirrored on the backend CDN). `requiredRamMb` is our own gate (the allowlist carries no RAM field).
 */
object AiModelCatalog {

    private const val HF = "https://huggingface.co"

    val MODELS: List<AiModelDescriptor> = listOf(
        AiModelDescriptor(
            id = "gemma3-1b-it",
            name = "Gemma 3 1B",
            family = "gemma3",
            parameterLabel = "1B",
            role = ModelRole.CHAT,
            fileName = "gemma3-1b-it-int4.litertlm",
            sizeBytes = 584_417_280L,
            sha256 = null,
            requiredRamMb = 3072,
            backendId = "litert-lm",
            platforms = setOf(ModelPlatform.ANDROID, ModelPlatform.IOS, ModelPlatform.DESKTOP),
            recommended = true,
            sourceUrl = "$HF/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.litertlm",
        ),
        AiModelDescriptor(
            id = "gemma-3n-E2B-it",
            name = "Gemma 3n E2B",
            family = "gemma3n",
            parameterLabel = "E2B",
            role = ModelRole.CHAT,
            fileName = "gemma-3n-E2B-it-int4.litertlm",
            sizeBytes = 3_655_827_456L,
            sha256 = null,
            requiredRamMb = 4096,
            backendId = "litert-lm",
            platforms = setOf(ModelPlatform.ANDROID, ModelPlatform.IOS, ModelPlatform.DESKTOP),
            recommended = false,
            sourceUrl = "$HF/google/gemma-3n-E2B-it-litert-lm/resolve/main/gemma-3n-E2B-it-int4.litertlm",
        ),
        AiModelDescriptor(
            id = "gemma-3n-E4B-it",
            name = "Gemma 3n E4B",
            family = "gemma3n",
            parameterLabel = "E4B",
            role = ModelRole.CHAT,
            fileName = "gemma-3n-E4B-it-int4.litertlm",
            sizeBytes = 4_919_541_760L,
            sha256 = null,
            requiredRamMb = 6144,
            backendId = "litert-lm",
            platforms = setOf(ModelPlatform.ANDROID, ModelPlatform.IOS, ModelPlatform.DESKTOP),
            recommended = false,
            sourceUrl = "$HF/google/gemma-3n-E4B-it-litert-lm/resolve/main/gemma-3n-E4B-it-int4.litertlm",
        ),
        AiModelDescriptor(
            id = "qwen2.5-1.5b-instruct",
            name = "Qwen2.5 1.5B Instruct",
            family = "qwen2.5",
            parameterLabel = "1.5B",
            role = ModelRole.CHAT,
            fileName = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
            sizeBytes = 1_597_931_520L,
            sha256 = null,
            requiredRamMb = 3072,
            backendId = "litert-lm",
            platforms = setOf(ModelPlatform.ANDROID, ModelPlatform.IOS, ModelPlatform.DESKTOP),
            recommended = false,
            sourceUrl = "$HF/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
        ),
        AiModelDescriptor(
            id = "phi-4-mini-instruct",
            name = "Phi-4 Mini Instruct",
            family = "phi4",
            parameterLabel = "3.8B",
            role = ModelRole.CHAT,
            fileName = "Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm",
            sizeBytes = 3_910_090_752L,
            sha256 = null,
            requiredRamMb = 6144,
            backendId = "litert-lm",
            platforms = setOf(ModelPlatform.ANDROID, ModelPlatform.IOS, ModelPlatform.DESKTOP),
            recommended = false,
            sourceUrl = "$HF/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm",
        ),
        AiModelDescriptor(
            id = "deepseek-r1-distill-qwen-1.5b",
            name = "DeepSeek-R1 Distill Qwen 1.5B",
            family = "deepseek-r1",
            parameterLabel = "1.5B",
            role = ModelRole.CHAT,
            fileName = "DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.litertlm",
            sizeBytes = 1_833_451_520L,
            sha256 = null,
            requiredRamMb = 3072,
            backendId = "litert-lm",
            platforms = setOf(ModelPlatform.ANDROID, ModelPlatform.IOS, ModelPlatform.DESKTOP),
            recommended = false,
            sourceUrl = "$HF/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.litertlm",
        ),
        // Tool-caller (intent → action), not a chat model. taskType `llm_tiny_garden` in the allowlist;
        // exposed with role INTENT so the app wires it as the function-calling model, not the chat pick.
        AiModelDescriptor(
            id = "function-gemma-270m",
            name = "FunctionGemma 270M",
            family = "gemma",
            parameterLabel = "270M",
            role = ModelRole.INTENT,
            fileName = "tiny_garden.litertlm",
            sizeBytes = 288_440_320L,
            sha256 = null,
            requiredRamMb = 2048,
            backendId = "litert-lm",
            platforms = setOf(ModelPlatform.ANDROID, ModelPlatform.IOS, ModelPlatform.DESKTOP),
            recommended = false,
            sourceUrl = "$HF/google/functiongemma-270m-it/resolve/main/tiny_garden.litertlm",
        ),
    )

    fun byId(id: String): AiModelDescriptor? = MODELS.firstOrNull { it.id == id }
}
