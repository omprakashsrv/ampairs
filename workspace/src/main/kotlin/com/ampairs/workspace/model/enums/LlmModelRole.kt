package com.ampairs.workspace.model.enums

/**
 * The role a downloadable on-device LLM plays in the mobile assistant pipeline.
 * Mirrors the app's `com.ampairs.agent.llm.ModelRole` (serialized by name).
 */
enum class LlmModelRole {
    INTENT,
    CHAT,
    FALLBACK,
}
