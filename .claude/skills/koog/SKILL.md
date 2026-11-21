---
name: koog
description: "Koog (JetBrains Kotlin AI-agent framework) reference for the Ampairs backend. Use when building or reviewing server-side AI-agent features in the :agent module — cloud chat, tool-calling over Ampairs domain services, structured output, RAG, or replacing the Anthropic-SDK chat proxy. Backend/JVM only — NOT the app's on-device LiteRT path."
trigger: /koog
---

# /koog — Koog on the Ampairs backend

> **Baseline:** Koog `1.1.1` (latest; `1.0.0` = first stable, May 2026, 1-year API-stability guarantee) ·
> JDK 17+ (we run 21) · Spring Boot 4.1 · Kotlin 2.4. Confirm the version on
> [Maven Central `ai.koog`](https://central.sonatype.com/search?q=ai.koog) / the
> [releases page](https://github.com/JetBrains/koog/releases) before pinning.
> Docs: <https://docs.koog.ai> (source: [`JetBrains/koog/docs`](https://github.com/JetBrains/koog/tree/main/docs/docs)).
> The canonical, always-current LLM reference is `https://docs.koog.ai/llms-full.txt` — feed it to a
> coding agent when this skill is not enough.

**What Koog is:** JetBrains' open-source Kotlin framework for building agents — tool-calling, graph
strategies, structured output, memory/RAG, history compression, OpenTelemetry observability — over
network LLM clients (OpenAI, Anthropic, Google, DeepSeek, OpenRouter, Ollama, Bedrock, Mistral,
Alibaba). It is a **JVM/backend fit here.** Read *When to reach for Koog* before adding it.

> **Scope note (AI Ops Manager).** Koog governs only the **optional, deferred backend tier** of the AI
> Business Operations Manager. The near-term Ops-Manager orchestrator runs **app-side** and uses the
> app's own `LlmEngine` — **not** Koog (ADR 0005). Both hosts serve the same host-agnostic engine SPI
> (ADR 0006). This skill's "backend/JVM only, not the app" framing is about **Koog the library**, and is
> consistent with that split — see [`docs/ai-ops-manager/README.md`](../../docs/ai-ops-manager/README.md).

---

## 0. Scope — where Koog belongs in Ampairs (read first)

| Path | Koog? | Why |
|---|---|---|
| App **on-device** brain (LiteRT-LM / Gemma, `feature/agent`) | ❌ Never | Koog has no on-device runtime. Do not touch the mobile pipeline. |
| App **cloud tier** (`CloudLlmEngine` → `AmpairsProxyTransport` → us) | ⚠️ Indirect | The app already proxies through us. Koog lives **here on the backend**, behind the same `POST /agent/v1/chat/completions` surface the app already calls. |
| **Backend `:agent` module** — cloud chat, tool-calling, RAG, structured answers | ✅ Primary | Keys stay server-side (already `agent.anthropic-api-key`), we own the proxy, the JVM builds locally, and Koog gives real agentic orchestration. |

**Golden rule:** Koog changes stay **inside `:agent`** behind the existing `/agent/v1/**` contract, so
the mobile app needs **no release** to benefit. Never widen the app-facing wire contract without a
matching client change.

### When to reach for Koog (vs. the current Anthropic SDK)
The module today calls the hosted model with the **official Anthropic Java SDK**
(`com.anthropic:anthropic-java`) in `AiChatProxyService` — a single request/response completion. Reach
for Koog only when you need something that SDK does **not** give cheaply:
- multi-step **tool-calling** over Ampairs domain data (agent decides, calls a tool, continues),
- typed **structured output** with auto-repair,
- **RAG** / memory / history compression,
- provider-agnostic **multi-LLM** fallback + **OpenTelemetry** tracing.

A plain single-shot completion does **not** need Koog — leave `AiChatProxyService` as is.

---

## 1. Gradle setup (`agent/build.gradle.kts`)

Two entry points — pick one:

```kotlin
// Option A — Spring Boot starter: auto-configures PromptExecutor beans from properties.
// NOTE: this artifact has no plain "1.1.1" release — Maven Central only ships it as a
// "-beta" build. Treat it as beta-stability even though koog-agents itself is stable.
implementation("ai.koog:koog-spring-boot-starter:1.1.1-beta")

// Option B — plain library: you build executors/agents yourself (more control, no auto-config).
implementation("ai.koog:koog-agents:1.1.1")
```

`mavenCentral()` is already the repo. **Do not hardcode versions elsewhere** — if the project adopts a
version catalog for the backend, add `koog` there. Koog pulls its own Ktor + kotlinx.serialization;
after adding it run `./gradlew :agent:dependencies` and confirm no clash with Spring's managed
versions. Keep Koog confined to `:agent` (module-boundary rule 08 — `:agent` depends only on `:core`;
reach other modules' data through their **public service interfaces**, never their repositories).

---

## 2. Spring Boot wiring (Option A)

Config keys (put secrets in env vars per security rule 10 — never commit them):

```yaml
# application.yaml
ai:
  koog:
    anthropic:
      enabled: true
      api-key: ${AGENT_ANTHROPIC_API_KEY}     # reuse the existing server-side key
      base-url: https://api.anthropic.com
    # openai: { enabled: true, api-key: ${OPENAI_API_KEY} }   # add providers as needed
```

Auto-configured beans (present only when the matching `api-key` is set): `anthropicExecutor`,
`openAIExecutor`, `googleExecutor`, `openRouterExecutor`, `deepSeekExecutor`, `ollamaExecutor`. These
are the **only** six providers the Spring Boot starter wires up — Mistral, Bedrock, and Alibaba clients
exist in `koog-agents` (Option B) but are **not** auto-configured by the starter, and there is no
`multiLLMPromptExecutor` fallback bean. Inject by type when one exists, or disambiguate with
`@Qualifier`:

```kotlin
@Service
class AgentChatService(
    @Qualifier("anthropicExecutor") private val executor: PromptExecutor,
) { /* ... */ }
```

---

## 3. Ampairs conventions Koog code MUST follow

Koog is a library, not an excuse to break house rules. In `:agent`:

- **Controllers return `ApiResponse<T>`** (rule 04); business exceptions bubble to the global handler —
  **no try/catch in controllers**. Wrap Koog calls in a **service**, and let failures propagate (the
  `try/catch → ResponseEntity.internalServerError()` shown in Koog's own docs is **not** our style).
- **DTO isolation (rule 02):** never return Koog types or JPA entities from a controller. Map to a
  Request/Response DTO in `agent/domain/dto/`.
- **JSON is global SNAKE_CASE (rule 03):** DTO fields stay camelCase in Kotlin; no `@JsonProperty`.
- **Package layout (rule 08):** `com.ampairs.agent.{domain|repository|service|controller}`. Koog
  services live in `com.ampairs.agent.service`, tool classes in `com.ampairs.agent.service.tools` (or a
  clearly-named subpackage).
- **Suspend + MVC:** Koog's `execute`/`run` are `suspend`. Spring MVC supports `suspend` controller
  methods; keep the whole call chain `suspend` rather than `runBlocking`.

### ⚠️ Multi-tenancy is the sharp edge (rules 05 + 06)
`TenantContextHolder` is thread-bound and set **at the controller level** in a `try/finally`. Koog runs
tool calls on coroutine dispatchers that **may switch threads**, so a tool that calls an Ampairs
service can lose the tenant context and read the wrong (or no) tenant's data.

- **Preferred:** capture the `workspaceId` (from the security context / `X-Workspace-ID`) in the
  controller and **pass it explicitly** into the service that builds the agent, then into each tool —
  don't rely on the agent's threads inheriting a ThreadLocal.
- If you must rely on `TenantContextHolder` inside tools, propagate it with a coroutine
  `ThreadContextElement` (or run the agent on a context that restores it). Verify with a two-tenant
  test that tool reads are correctly scoped.
- Services still **never** call `TenantContextHolder.setCurrentTenant()` (rule 05) — that stays a
  controller responsibility.

---

## 4. Prompt executor + prompt DSL (simplest useful call)

```kotlin
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.model.PromptExecutor

@Service
class AgentChatService(private val executor: PromptExecutor) {
    suspend fun answer(userMessage: String): String {
        val p = prompt("ampairs-chat") {
            system("You are the Ampairs business assistant. Be concise and accurate.")
            user(userMessage)
        }
        return executor.execute(p, AnthropicModels.Haiku_3_5).first().content
    }
}
```

Pin the model deliberately (small/cheap for intent, larger for chat) — mirror the app's INTENT vs CHAT
role split described in `agent/CLAUDE.md`.

---

## 5. Tools — expose Ampairs data to the agent

Define tools with `@Tool` + `@LLMDescription` inside a `ToolSet`. Inject the Ampairs **public service
interfaces** the tool needs (rule 08) and pass the tenant scope in explicitly (§3).

```kotlin
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet

@LLMDescription("Read-only lookups over the current workspace's business data")
class AmpairsToolSet(
    private val workspaceId: String,               // captured in the controller (§3)
    private val customerService: CustomerService,  // a :customer PUBLIC interface
) : ToolSet {

    @Tool
    @LLMDescription("Count active customers in the current workspace")
    fun countCustomers(): String =
        customerService.countActive(workspaceId).toString()

    @Tool
    @LLMDescription("Find customers whose name matches a query")
    fun findCustomers(
        @LLMDescription("Case-insensitive name fragment to search for")
        query: String,
    ): String = customerService.searchByName(workspaceId, query).joinToString("\n") { it.name }
}
```

Register in a `ToolRegistry` (Koog reflects the annotated methods via `asTools()`):

```kotlin
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.core.tools.ToolRegistry

val toolRegistry = ToolRegistry {
    tools(AmpairsToolSet(workspaceId, customerService).asTools())
}
```

**Tool discipline:**
- Keep tools **read-only** unless a write is explicitly designed, reviewed, and idempotent — an LLM
  deciding to mutate tenant data is high-risk.
- Every tool is scoped to `workspaceId`; no tool should be able to reach another tenant's rows.
- Descriptions are the contract the model reads — be precise about units, and note when a value is
  stored differently than displayed (e.g. minor units / paise), like the app's query-schema rules.

---

## 6. AIAgent — single-run and beyond

```kotlin
import ai.koog.agents.core.agent.AIAgent

val agent = AIAgent(
    promptExecutor = executor,
    systemPrompt = "You are the Ampairs business assistant. Use tools for any data question.",
    llmModel = AnthropicModels.Haiku_3_5,
    temperature = 0.2,
    toolRegistry = toolRegistry,
    maxIterations = 8,              // cap the tool-call loop — cost & safety
) {
    handleEvents {
        onToolCallStarting { e -> logger.info("tool ${e.toolName} args=${e.toolArgs}") }
    }
}

val reply: String = agent.run(userMessage)   // suspend; drives the tool loop to completion
```

- Default construction gives a **single-run** agent (one tool-calling loop, bounded by
  `maxIterations`). For multi-stage flows use **graph strategies** (`custom-strategy-graphs.md`,
  `agents/graph-based-agents.md`) — nodes for request/tool/decision. Don't reach for graphs until a
  single-run agent is proven insufficient.
- `maxIterations` is a real cost/runaway guard — set it low and raise deliberately.

---

## 7. Structured output (typed, self-repairing answers)

Use when the app needs a machine-readable result, not prose:

```kotlin
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("SalesSummary")
@LLMDescription("Aggregated sales figures for a period")
data class SalesSummary(
    @property:LLMDescription("Total sales amount in workspace currency, minor units")
    val totalMinor: Long,
    @property:LLMDescription("Number of invoices counted")
    val invoiceCount: Int,
)

val result = executor.executeStructured<SalesSummary>(
    prompt = prompt("sales-summary") { system(schemaSystemPrompt); user(question) },
    model = AnthropicModels.Haiku_3_5,
    fixingParser = StructureFixingParser(fixingModel = AnthropicModels.Sonnet_4_5, retries = 3),
)
```

Map the parsed value into an Ampairs **Response DTO** before returning (rule 02). Note these are Koog's
`@LLMDescription` (from `kotlinx.serialization` structuring) — distinct from the JSON wire contract.

---

## 8. Further Koog features (pointers, not detail)
- **RAG / memory / embeddings** — `retrieval-augmented-generation.md`, embeddings docs; back it with a
  real vector store, scoped per tenant.
- **History compression** — `history-compression.md`, for long conversations.
- **Observability** — OpenTelemetry across all targets; wire it into the existing actuator/metrics
  setup.
- **Ktor / MCP / Spring AI 2.0 interop** — `ktor-plugin.md`, `model-context-protocol.md`,
  `spring-ai-integration.md`.

---

## 9. Migration path — Anthropic SDK → Koog (incremental, low-risk)
1. Add `koog-spring-boot-starter`; set `ai.koog.anthropic.api-key` to the existing server key. **Change
   nothing else** — confirm the module still builds and tests pass.
2. Add a Koog-backed `AgentChatService` **behind a feature flag / new internal method**, leaving
   `AiChatProxyService` (Anthropic SDK) as the default path.
3. Prove parity on the existing `POST /agent/v1/chat/completions` shape (same request/response DTOs).
4. Introduce **one read-only tool** (e.g. `countCustomers`) and a two-tenant test verifying scoping
   (§3) before adding more.
5. Only after tools are trusted, consider structured output / RAG. Retire the Anthropic SDK path only
   when Koog covers every case in use.

Keep the **app-facing contract identical** at every step.

---

## 10. Testing & build
- `./gradlew :agent:compileKotlin :agent:test` builds locally (system JDK 21) — do this before pushing
  (the app repo can't build in-sandbox, but the backend can).
- Unit-test **tools** as plain functions (no LLM). Test the **service** with a fake/stub
  `PromptExecutor` — don't hit a real provider in CI (no keys, non-deterministic, costs money). See
  Koog `testing.md` for its test harness.
- Add a **two-tenant scoping test** for any tool that reads tenant data (§3) — this is the failure mode
  most likely to slip through.

---

## Gotchas checklist
- [ ] Koog stays inside `:agent`; cross-module data via **public service interfaces** only (rule 08).
- [ ] Controllers return `ApiResponse<T>`, no try/catch; Koog calls live in a service (rule 04/05).
- [ ] No Koog types / JPA entities leak into responses — map to `domain/dto/` DTOs (rule 02).
- [ ] `workspaceId` passed **explicitly** into agent + tools; two-tenant test proves scoping (rule 05).
- [ ] Tools are read-only unless a write is deliberately designed & reviewed.
- [ ] `maxIterations` set low; provider key from env, never committed (rule 10).
- [ ] App-facing `/agent/v1/**` contract unchanged (no forced app release).
- [ ] `./gradlew :agent:compileKotlin :agent:test` green before pushing.
- [ ] Koog version confirmed on Maven Central; no version clash in `:agent:dependencies`.
