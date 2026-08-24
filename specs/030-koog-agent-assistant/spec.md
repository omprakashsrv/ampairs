# 030 — Server-side AI assistant: tool-calling data agent (Koog)

**Status:** Stub · **Blocked on:** the Koog backend spike (`docs/spikes/koog-agent-backend-spike.md`)
**Modules:** `agent` (primary), `core`, read-only tool access to `customer` (then `product`, `invoice`, `order`, `payment`) via public service interfaces
**Related:** `.claude/skills/koog/SKILL.md` · `docs/spikes/koog-agent-backend-spike.md` · PR #221 · module doc `docs/modules/agent.md`

> **This is a placeholder, not a ready-to-plan spec.** It reserves the `030` number and records intent.
> Do **not** run `/speckit.plan` / `/speckit.tasks` against it until the spike in
> `docs/spikes/koog-agent-backend-spike.md` returns a **Go** at its §10 decision gate. On Go, expand
> this file via `/speckit.specify` (then clarify → plan → tasks); on No-go, delete this directory.

---

## 1. Summary

Add a **server-side, tool-calling AI assistant** to the `:agent` module: a natural-language endpoint
that answers a workspace's business-data questions ("how many active customers?", "top products by
sales this month?", "what's my outstanding from customer X?") by having a hosted LLM **call read-only
tools** over that workspace's real data through existing public service interfaces, grounded and
tenant-scoped — powered by [Koog](https://docs.koog.ai).

This is **additive**. It does not replace `POST /agent/v1/chat/completions` (already provider-agnostic +
structured output) or the app's on-device LiteRT path.

## 2. Why (gap this closes)

Neither existing path can decide *"I need to look this up"*, call a function against live workspace
data, read the result, and continue:
- the cloud proxy answers one-shot from the prompt only;
- on-device tiny models do constrained text-to-SQL but are limited by model size and require a loaded model.

A server-side agent with real, read-only tools gives grounded data answers with no app release (it sits
behind the existing `/agent/v1/**` namespace).

## 3. Scope (provisional — finalized after the spike)

**In (initial release):**
- Feature-flagged, **tenant-scoped** `POST /agent/v1/assistant/query` returning `ApiResponse<…>`.
- Koog `AIAgent` with **read-only** tools over a growing set of modules (start: customer; then product,
  invoice, order, payment) — each tool reaches its module through the **public service interface** only.
- Correct tenant propagation across Koog's coroutine threads (the spike's core finding, §5 there).
- Bounded tool loop (`maxIterations`), server-side keys, OpenTelemetry tracing.

**Out (at least initially):**
- Any write/mutation tool; RAG/memory/history-compression (possible later phases); streaming;
  changes to `/chat/completions`; mobile-app changes (the app-side surface is a separate follow-up).

## 4. Hard constraints (house rules that pre-shape this spec)
- Controllers return `ApiResponse<T>`, no business try/catch (rules 04/05); Koog calls live in a service.
- DTO isolation (rule 02): no Koog/JPA types on the wire; DTOs in `agent/domain/dto/`.
- Multi-tenancy (rules 05/06): every tool read scoped to the caller's workspace; **two-tenant scoping
  test mandatory**. Services never set tenant context.
- Module boundaries (rule 08): Koog confined to `:agent`; cross-module data via public interfaces only.
- Secrets from env (rule 10); reuse the existing server-side provider key.

## 5. Open questions (resolve during clarify, informed by spike findings)
- Tenant propagation mechanism: `ThreadContextElement` (keeps `@TenantId` services) vs. explicit-scope
  `nativeQuery` tools — spike recommends the former.
- Execution model under load: `runBlocking` on the MVC thread (spike) vs. async/WebFlux (GA).
- Which modules ship read tools in v1, and the exact public-interface methods each needs.
- Conversation memory: stateless per-query (v1) vs. session memory (later).
- Answer-quality bar + eval set; per-query cost/latency budget and model tiering (Haiku vs Sonnet).

## 6. Success criteria (inherit + extend the spike's exit gate)
- End-to-end: the endpoint answers data questions via tool calls against a real model.
- No cross-tenant leakage (two-tenant test green).
- `/chat/completions` and existing `:agent` tests unchanged and green.
- Documented answer quality on an agreed eval set, within an agreed cost/latency budget.

---

_Next step once the spike says Go: `/speckit.specify` to flesh out §3–§6, then `/speckit.clarify`._
