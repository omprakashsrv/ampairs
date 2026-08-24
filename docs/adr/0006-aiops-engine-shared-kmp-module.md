# 0006 — AI Ops engine is a host-agnostic shared KMP module

- Status: Accepted
- Date: 2026-08-24
- Related: `docs/ai-ops-manager/framework.md` (§3 SPI), ADR 0005 (app-side orchestration), ADR 0002

## Context

ADR 0005 puts the AI Ops Manager orchestrator on the app, but the same capabilities
(detect → propose → score → gate → apply → audit) may later need to run on the backend for always-on
autonomy and admin cross-tenant work. We do not want two divergent engines, and we do not want the
decision logic coupled to either Koog (JVM-only) or to Android/JVM APIs.

## Decision

The engine is a **host-agnostic Kotlin SPI** — plain `commonMain` types, **no Koog and no `java.*`/
`android.*` in the contract** — living in a shared KMP module (`feature/aiops` in `ampairs-app` now;
structured so it can be published for a future backend host).

The pipeline (`Detector → ContextGatherer → CandidateGenerator → CandidateValidator → ConfidenceScorer
→ RiskPolicy → Executor → AuditWriter → FeedbackStore`) is fixed and shared. The platform-specific parts
are two **ports** each host implements:

- **`Reasoner`** — the LLM judgment port. App = the existing `LlmEngine` (on-device + cloud tier);
  backend (later) = Koog.
- **`Executor`** — how a correction is applied. App = repository write + offline-sync; backend (later) =
  the owning module's public service.

A "capability" (unit standardization, dedup, HSN advisory…) is one set of stage plug-ins registered
under a key; adding one never edits the engine.

## Consequences

- **Positive:** one engine, two hosts; capabilities are portable; the app ships now while the backend
  tier stays a wiring exercise, not a rewrite; the LLM is a swappable port, so on-device vs cloud vs
  Koog is a host choice, not an engine change.
- **Negative / open follow-up:** whether the shared `aiops-engine` is **published** (like
  `data-common`/`auth`, so the backend consumes the *same* artifact) or kept app-only with the backend
  re-declaring the SPI is unresolved — tracked as framework §18.4. Decide before the backend tier is
  built to avoid drift.
- **Guardrail:** anything JVM/Koog-specific must live behind the `Reasoner`/`Executor` ports, never in
  the shared engine types, or the module stops compiling for iOS/Wasm.
