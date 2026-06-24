package com.ampairs.agent.domain.model

/**
 * Target platform an on-device model can run on. The mobile app filters/disables models whose
 * [AiModelDescriptor.platforms] does not include the running platform.
 */
enum class ModelPlatform { ANDROID, IOS, DESKTOP }

/**
 * One downloadable on-device LLM (a LiteRT-LM `.litertlm` Gemma checkpoint). This is curated
 * reference data, not a tenant entity — it lives in code, not the DB (no per-workspace overrides,
 * changes ship with backend releases).
 *
 * The app downloads the bytes through the backend proxy (`GET /agent/v1/models/{id}/download`), so
 * [sourceUrl] is intentionally **server-side only** and never leaves the backend in the manifest.
 *
 * @property id stable identifier used in the download path and persisted by the app as the selected model.
 * @property fileName the on-disk filename the app stores and the LiteRT-LM engine loads — must match
 *   exactly what the engine expects (`filesDir/agent_models/{fileName}`).
 * @property sizeBytes approximate download size, for the app's pre-download size warning. The proxy's
 *   `Content-Length` is authoritative for actual progress.
 * @property sha256 lowercase hex digest for post-download integrity verification, or null to skip
 *   verification (TODO: fill in real digests once mirrored on the backend CDN).
 * @property requiredRamMb minimum device total RAM (MB) to run this model; the app disables selection
 *   below this and shows a "needs X / device has Y" message.
 * @property backendId which app-side LLM backend runs it (`litert-lm`).
 * @property sourceUrl upstream URL the backend proxy streams from (HuggingFace litert-community).
 */
data class AiModelDescriptor(
    val id: String,
    val name: String,
    val family: String,
    val parameterLabel: String,
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
 * NOTE: [AiModelDescriptor.sourceUrl] / [AiModelDescriptor.sha256] / [AiModelDescriptor.sizeBytes]
 * are best-known values for the LiteRT-LM `litert-community` Gemma repos and should be verified
 * against HuggingFace before GA — a wrong URL only fails the runtime download, never the build.
 */
object AiModelCatalog {

    private const val HF = "https://huggingface.co"

    // sourceUrl / fileName / sizeBytes are verified against the Google AI Edge Gallery
    // model_allowlists (the canonical LiteRT-LM Gemma manifest) — modelId + modelFile + sizeInBytes
    // copied verbatim so the proxy resolves a real file and the app's exact-size check passes.
    // sizeBytes MUST match the upstream file byte-for-byte: the app fails the download otherwise.
    val MODELS: List<AiModelDescriptor> = listOf(
        AiModelDescriptor(
            id = "gemma3-1b-it",
            name = "Gemma 3 1B",
            family = "gemma3",
            parameterLabel = "1B",
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
            id = "gemma-4-E2B-it",
            name = "Gemma 4 E2B",
            family = "gemma4",
            parameterLabel = "E2B",
            fileName = "gemma-4-E2B-it.litertlm",
            sizeBytes = 2_588_147_712L,
            sha256 = null,
            requiredRamMb = 6144,
            backendId = "litert-lm",
            platforms = setOf(ModelPlatform.ANDROID, ModelPlatform.IOS, ModelPlatform.DESKTOP),
            recommended = true,
            sourceUrl = "$HF/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        ),
        AiModelDescriptor(
            id = "gemma-4-E4B-it",
            name = "Gemma 4 E4B",
            family = "gemma4",
            parameterLabel = "E4B",
            fileName = "gemma-4-E4B-it.litertlm",
            sizeBytes = 3_659_530_240L,
            sha256 = null,
            requiredRamMb = 8192,
            backendId = "litert-lm",
            platforms = setOf(ModelPlatform.ANDROID, ModelPlatform.IOS, ModelPlatform.DESKTOP),
            recommended = true,
            sourceUrl = "$HF/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
        ),
    )

    fun byId(id: String): AiModelDescriptor? = MODELS.firstOrNull { it.id == id }
}
