# Spike: Koog server-side agent in the `:agent` module

**Status:** Proposed (time-boxed spike, not a commitment to adopt)
**Owner:** _TBD_
**Related:** `.claude/skills/koog/SKILL.md` · module doc `docs/modules/agent.md` · PR #221
**Time-box:** ~4–5 engineering days
**Decision gate:** §10 — go/no-go on adopting Koog for a tool-calling data assistant

---

## 1. Summary

Prove — behind a feature flag, in a throwaway-friendly branch — that a **Koog**-driven agent running
inside the `:agent` module can answer a natural-language *business-data* question ("how many active
customers do I have?", "top 5 products by sales this month?") by having the LLM **call read-only tools**
over the asking workspace's data, and return a grounded answer. The spike succeeds or fails on one
question: **can Koog's tool-calling loop be wired to Ampairs' tenant-scoped services safely and
correctly**, without regressing the existing cloud chat path.

This is explicitly *not* a rewrite of the current chat proxy. It adds a **new, additive** capability
the current stack does not have.

---

## 2. Motivation — what we have vs. what's missing

The `:agent` module today already does more than a naive proxy:

- `POST /agent/v1/chat/completions` (`AiChatController` → `AiChatProxyService`) is a **one-shot,
  provider-agnostic** completion. A `ChatProvider` interface already routes by model allow-list
  (`AnthropicChatProvider`, `OpenRouterChatProvider`), keys stay server-side, and **structured output
  already works** by injecting the JSON schema into the system prompt (`ChatPrompt`).
- So "provider abstraction", "keys server-side", and "structured JSON output" are **already solved** —
  Koog would not add them.

What the current path **cannot** do, and what motivates the spike:

- **Multi-step tool-calling.** The hosted model answers from the prompt alone. It cannot decide *"I need
  to look this up"*, call a function against the workspace's real data, read the result, and continue.
  The on-device tiny models (LiteRT) can't do this reliably either — that's the whole reason a
  server-side agent is interesting.
- **Grounded data answers.** Today a data question is answered on-device via the constrained
  text-to-SQL path (see app `feature/agent`), which needs a loaded model and is limited by model size.
  A server-side agent with real tools sidesteps both.

Koog gives the tool-calling loop, `maxIterations` safety bound, typed structured output with
auto-repair, and OpenTelemetry tracing "for free". The question is whether it fits our multi-tenant,
blocking-MVC, module-bounded backend.

**If the spike shows tool-calling is fragile or the tenant-propagation cost is high, we do not adopt
Koog** and keep the current proxy.

---

## 3. Scope

### In scope (the spike builds exactly this)
1. Add `koog-agents` to `:agent` (library option, not the Spring starter — see §6).
2. One new **feature-flagged**, **tenant-scoped** endpoint: `POST /agent/v1/assistant/query`.
3. A Koog `AIAgent` with **one or two read-only tools** over **one** existing module's public service
   (customer is the reference — smallest surface, already queryable in the app).
4. Correct **tenant propagation** into tool execution (§5 — the crux).
5. Tests: tool unit tests + a **two-tenant scoping test** + a service test with a fake executor.

### Explicitly out of scope (do NOT build in the spike)
- Any change to `POST /agent/v1/chat/completions` or its DTOs.
- Any change to the mobile app (the app calls this only if/when we ship it; the spike is backend-only).
- RAG / embeddings / vector store / conversation memory / history compression.
- **Any write/mutation tool.** Read-only only.
- Multi-provider fallback, streaming (SSE), and the Spring Boot starter's auto-config.
- Adopting Koog anywhere outside `:agent`.

---

## 4. Proposed architecture

```
app (optional, later)                         backend :agent module
─────────────────────         ┌───────────────────────────────────────────────────────────┐
POST /agent/v1/assistant/query │  AssistantQueryController  (tenant-scoped, feature-flagged) │
  { "message": "..." }  ──────►│    · reads X-Workspace-ID / security ctx → workspaceId      │
                               │    · returns ApiResponse<AssistantQueryResponse>            │
                               │            │                                                │
                               │            ▼                                                │
                               │  AssistantAgentService (@Service)                           │
                               │    · builds ToolRegistry(workspaceId)                       │
                               │    · runs AIAgent within a TenantContext coroutine element  │
                               │            │                                                │
                               │   ┌────────┴─────────┐                                      │
                               │   ▼                  ▼                                      │
                               │ PromptExecutor   AmpairsReadToolSet(workspaceId)            │
                               │ (Anthropic)        · countCustomers()                       │
                               │                    · findCustomers(query)                   │
                               │                         │ calls PUBLIC service iface        │
                               │                         ▼                                   │
                               │              CustomerService (:customer, @TenantId scoped)  │
                               └───────────────────────────────────────────────────────────┘
```

Key placement decisions:
- **New endpoint, not an overload of `/chat/completions`.** The chat proxy is intentionally *not*
  tenant-scoped; a data agent *must* be. Keeping them separate avoids conflating two security postures.
- **Namespace:** `com.ampairs.agent.service.assistant` for the service + tools; `…controller` for the
  controller; DTOs in `…domain.dto`.
- **Cross-module access via public service interface only** (module-boundary rule 08). `:agent` depends
  only on `:core` today; the spike adds a dependency on the **`:customer` public service interface**
  (not its repository). Confirm such an interface exists / is exported before starting; if it doesn't,
  that surface work is part of the spike's cost and a finding in itself.

### Endpoint contract (spike)

```
POST /agent/v1/assistant/query          (JWT + X-Workspace-ID required; tenant-scoped)
Request  : { "message": "how many active customers do I have?" }
Response : ApiResponse<AssistantQueryResponse>
           { "answer": "You have 128 active customers.",
             "tool_calls": ["countCustomers"],      // spike-only, for evaluation
             "model_id": "claude-haiku-4-5" }
```

`tool_calls` is a spike diagnostic (captured via Koog `handleEvents`); drop it before any GA.

---

## 5. The crux: tenant propagation across Koog's coroutine threads

This is the make-or-break design problem and the main thing the spike must de-risk.

- Ampairs tenant scoping is **thread-bound**: `TenantContextHolder` (a ThreadLocal) is set at the
  controller and read by JPA's `@TenantId` filter (rules 05/06). Services **never** set it (rule 05).
- Koog's `AIAgent.run` is `suspend` and executes tool calls on **its own coroutine dispatchers**, which
  **switch threads**. A tool that calls `customerService.…` on a Koog worker thread will run with **no
  tenant context** → the `@TenantId` filter returns the wrong tenant's rows or none.

**Primary approach for the spike — `ThreadContextElement` that carries the tenant:**
Run the agent inside a coroutine context element that sets `TenantContextHolder` on *every* thread the
coroutine touches and restores it on suspension. This keeps **all existing `@TenantId` services working
unchanged** — the highest-value property.

```kotlin
// Sketch — not final. A ThreadContextElement that pins the tenant onto whatever thread runs the coroutine.
class TenantContextElement(private val workspaceId: String) :
    ThreadContextElement<String?> {
    companion object Key : CoroutineContext.Key<TenantContextElement>
    override val key get() = Key
    override fun updateThreadContext(context: CoroutineContext): String? {
        val prev = TenantContextHolder.getCurrentTenant()
        TenantContextHolder.setCurrentTenant(workspaceId)
        return prev
    }
    override fun restoreThreadContext(context: CoroutineContext, oldState: String?) {
        if (oldState == null) TenantContextHolder.clear()
        else TenantContextHolder.setCurrentTenant(oldState)
    }
}

// In the service (blocking MVC → bridge with runBlocking + the element):
fun query(workspaceId: String, message: String): AssistantQueryResponse =
    runBlocking(TenantContextElement(workspaceId)) {
        val answer = agent(workspaceId).run(message)
        AssistantQueryResponse(answer = answer, /* … */)
    }
```

> ⚠️ Verify the exact `TenantContextHolder` API (getter/clear names) against `:core` before coding —
> the names above are placeholders. Confirm it is a plain ThreadLocal (not, e.g., a
> `TransmittableThreadLocal` or request-scoped bean) so the element semantics hold.

**Secondary approach (fallback, per the koog skill):** ignore the ThreadLocal in tools entirely and
pass `workspaceId` into tools that call **cross-tenant `nativeQuery` service methods** with an explicit
workspace param. This avoids the coroutine-context subtlety but (a) needs new explicit-scope query
methods on each module service and (b) rule 05 warns against mixing `@TenantId` + explicit `workspaceId`
— so it's more invasive per tool. Prefer the ThreadContextElement.

**The two-tenant test is mandatory** either way: seed two workspaces, ask the same question as each,
assert each answer reflects only its own data. This test is the spike's core evidence.

---

## 6. Dependency & config

- Use **`ai.koog:koog-agents`** (library), not `koog-spring-boot-starter`. Reasons: the starter only
  ships as `-beta` today, it auto-wires six providers we don't all want, it has **no
  `multiLLMPromptExecutor` fallback bean**, and we already have our own provider routing. For a
  read-only spike we just build one `AnthropicLLMClient`/executor by hand and inject the existing
  server-side key.
- Pin the version in `agent/build.gradle.kts`; if a backend version catalog exists, add it there
  (no hardcoded versions elsewhere).
- Reuse the existing server-side Anthropic key (`agent.anthropic-api-key`) via `@Value` — **no new
  secret, nothing committed** (security rule 10).
- Feature flag: `agent.assistant.enabled` (default **false**). Controller returns 404/501 when off so
  the endpoint is inert in prod until we decide.
- After adding, run `./gradlew :agent:dependencies` and confirm Koog's bundled Ktor / kotlinx.serialization
  don't clash with Spring's managed versions — **a dependency-conflict finding is a valid spike outcome.**

---

## 7. Tools for the spike (read-only, customer module)

```kotlin
@LLMDescription("Read-only lookups over the current workspace's customer data")
class CustomerReadToolSet(
    private val workspaceId: String,          // captured in controller; also pins tenant via §5 element
    private val customerService: CustomerService,   // :customer PUBLIC interface
) : ToolSet {

    @Tool
    @LLMDescription("Count active customers in the current workspace")
    fun countCustomers(): String = customerService.countActive(/* tenant via context */).toString()

    @Tool
    @LLMDescription("Find customers whose name contains the query (max 20 results)")
    fun findCustomers(
        @LLMDescription("Case-insensitive name fragment") query: String,
    ): String = customerService.searchByName(query).take(20).joinToString("\n") { it.name }
}
```

- Exact service method names must be checked against the real `CustomerService` interface — adapt or add
  thin public methods as needed (and note the cost).
- Tool result strings feed straight back into the model — keep them compact and unambiguous; document
  units where storage ≠ display (mirror the app's query-schema discipline).

---

## 8. Testing plan

| Test | What it proves |
|---|---|
| `CustomerReadToolSet` unit tests (plain calls, mocked `CustomerService`) | tools return correct strings; no LLM involved |
| `AssistantAgentService` with a **fake `PromptExecutor`** that scripts a tool call | the agent loop wires the tool and composes an answer, deterministically, no network |
| **Two-tenant scoping test** (H2, two workspaces seeded) | §5 works — each workspace sees only its own data |
| Controller test: flag off → inert; flag on + missing `X-Workspace-ID` → rejected | feature-flag + tenant guard behave |

Never call a real provider in CI (no keys, non-deterministic, costs money). Local manual smoke test
against the real Anthropic key is fine for the author. Build gate: `./gradlew :agent:compileKotlin :agent:test`.

---

## 9. Risks & mitigations

| Risk | Likelihood | Mitigation |
|---|---|---|
| Tenant context lost across Koog threads → cross-tenant data leak | **High if unhandled** | §5 ThreadContextElement + mandatory two-tenant test; the whole spike is designed to surface this early |
| Koog Ktor/serialization clashes with Spring's managed deps | Medium | `:agent:dependencies` audit on day 1; a clash is a reportable finding |
| `:customer` lacks a usable public service method → boundary work | Medium | Verify the interface first; scope any additions as spike cost |
| `runBlocking` on a Tomcat request thread under load | Low (spike) | Acceptable for a flagged spike; note async/WebFlux as a GA follow-up, don't solve now |
| Koog 1.x young; API churn | Low | 1.0 carries a 1-year stability guarantee; pin the version |
| Cost/latency of a multi-iteration tool loop | Medium | `maxIterations` low (≤6); small model (Haiku) for the spike; measure and record |

---

## 10. Exit criteria & decision gate

**Spike succeeds if all hold:**
1. `POST /agent/v1/assistant/query` answers a data question by calling ≥1 tool, verified end-to-end
   against a real model locally.
2. The two-tenant scoping test passes — no cross-tenant leakage.
3. `./gradlew :agent:compileKotlin :agent:test` is green; no unresolved dependency conflict.
4. `/agent/v1/chat/completions` and all existing `:agent` tests are untouched and still pass.

**Deliverable of the spike:** this endpoint behind a default-off flag, the tests above, and a short
**findings write-up** appended here answering: tenant-propagation effort, dependency friction,
answer quality on ~10 sample questions, and rough per-query cost/latency.

**Decision:**
- **Go** → promote to a speckit feature (`/speckit.specify`, next `specs/###`) covering more modules'
  read tools, conversation memory, observability wiring, an async (non-`runBlocking`) execution model,
  and the app-side surface to call it.
- **No-go** → delete the branch; keep the current proxy. The koog skill and this doc remain as the
  record of *why*.

---

## 11. Rough task breakdown (~4–5 days)

1. **D1** — Add `koog-agents`, dependency audit, `AnthropicLLMClient`/executor bean from existing key;
   verify `TenantContextHolder` API + `CustomerService` public interface. *(de-risks §6, §5, §7 upfront)*
2. **D2** — `TenantContextElement` + `AssistantAgentService` (single-run agent, one tool); local smoke test.
3. **D3** — Controller + DTOs + feature flag; `CustomerReadToolSet` second tool.
4. **D4** — Tests (tool units, fake-executor service test, **two-tenant** test, controller/flag tests).
5. **D5** — Sample-question eval, cost/latency notes, findings write-up + decision recommendation.
