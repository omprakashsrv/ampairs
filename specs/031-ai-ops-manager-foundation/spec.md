# 031 — AI Business Operations Manager: foundation (`:aiops` engine)

**Status:** Stub · **Blocked on:** framework blueprint sign-off + Koog spike tenant-propagation result
**Modules:** new `aiops` (primary), `core`, `setting` (autonomy config), `event` (event-driven trigger), `unit` (first capability), Koog
**Related:** `docs/ai-ops-manager/framework.md` (authoritative design) · `.claude/skills/koog/SKILL.md` · `docs/spikes/koog-agent-backend-spike.md` · `specs/030-koog-agent-assistant` · PR #221

> **Placeholder — do not `/speckit.plan` yet.** Full design lives in `docs/ai-ops-manager/framework.md`.
> Resolve the framework's §16 open decisions (module name, async model) and take the Koog spike to a Go
> before expanding this via `/speckit.specify`. On abandonment, delete this directory.

---

## 1. Summary

Build **Phase 0** of the AI Business Operations Manager: the reusable **decision engine** and
capability SPI in a new `:aiops` bounded context, proven end-to-end by **one** low-risk capability
(**unit standardization**). This is the machine every later data-quality and operations capability plugs
into — it is deliberately *not* a feature by itself beyond the one proof capability.

## 2. Scope (Phase 0 only)

**In:**
- New `:aiops` module (`com.ampairs.aiops`), depending on `:core` + Koog; reads/writes business data via
  other modules' **public service interfaces** only.
- Engine SPI: `Detector → ContextGatherer → CandidateGenerator → CandidateValidator → ConfidenceScorer
  → RiskPolicy → (Executor | ReviewQueue) → AuditWriter → FeedbackStore` (framework §3).
- Confidence×risk gate + per-workspace autonomy level via `setting` (framework §4–5).
- Four tables — `aiops_finding`, `aiops_decision` (audit), `aiops_feedback`, `aiops_agent_run` —
  `OwnableBaseDomain`, `Instant`, Flyway both vendors, added to `migrationModules` (framework §6).
- Tenant-scoped scan runner carrying tenant context across Koog coroutine threads (framework §7).
- Review-queue + audit + **rollback** APIs under `/aiops/v1` (framework §10–11).
- **One capability**: `product.unit` standardization (KG/Kg/Kgs→KG, L/LTR/Litres→L) as the proof —
  highest confidence, lowest risk, uses `UnitService`/`UnitConversionService`.

**Out (later phases / own specs):** product & customer dedup, hierarchy fix, HSN/GST, contact card →
customer, inventory/purchase/sales anomalies, morning-brief orchestration, RAG/few-shot learning,
higher autonomy levels.

## 3. Hard constraints
- Bounded context + writes via owning-module public services only (rule 08; framework §2).
- Multi-tenancy: every scan runs in a per-workspace tenant scope; **engine-level two-tenant test
  mandatory** (rules 05/06; framework §7). LLM is one capped signal, never sole authority (framework §8).
- `ApiResponse<T>`, no controller try/catch (rule 04); DTOs in `aiops/domain/dto/` (rule 02); SNAKE_CASE
  (rule 03); `Instant` (rule 01); secrets from env (rule 10).
- Every mutation audited + reversible; default autonomy = Recommend (L1); auto-fix opt-in per capability.

## 4. Success criteria
- `product.unit` runs via all three triggers (event/scheduled/user) and, at L2, auto-fixes a
  high-confidence alias while routing an ambiguous one to the review queue.
- A human decision writes feedback + audit; a `revert` restores the prior value through `UnitService`.
- Two-tenant scan test green (no cross-tenant leakage).
- `./gradlew :aiops:compileKotlin :aiops:test` green; existing modules untouched.

## 5. Open questions
See `docs/ai-ops-manager/framework.md` §16 (module name, async/job model, vector store, merge
semantics, cost budget, reference data). These must be answered before `/speckit.plan`.

---

_Next step: sign off framework §16.1–2, then `/speckit.specify` to expand §2–§4 into an implementable spec._
