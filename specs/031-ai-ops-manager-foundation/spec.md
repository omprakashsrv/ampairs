# 031 — AI Business Operations Manager: **Phase B** (backend `:aiops` — cross-entity + scheduled)

**Status:** Stub · **Blocked on:** Phase A (app-side) learnings + framework §18 open decisions (async model, shared-engine packaging) + Koog spike Go
**Modules:** new `aiops` (backend), `core`, `setting` (autonomy), `event` (event trigger), read/write via `customer`/`product`/`unit`/tax public services, Koog
**Related:** `docs/ai-ops-manager/framework.md` (authoritative design, rev 2) · `.claude/skills/koog/SKILL.md` · `docs/spikes/koog-agent-backend-spike.md` · `specs/030-koog-agent-assistant` · PR #221

> **Reframed (rev 2).** The framework is now **app-first**: single-entity, implicit fixes (unit
> standardization, field validation, contact-card capture) ship in **`ampairs-app`** as **Phase A** —
> tracked there, not here. This spec is **Phase B**: the *backend* host for the work that genuinely needs
> a server — **whole-dataset/cross-entity** capabilities (dedup, authoritative contact→customer match)
> and **scheduled/continuous/autonomous** operation. Do not `/speckit.plan` until Phase A proves the
> shared engine, framework §18.3–4 are decided, and the Koog spike returns Go.

## 1. Summary
Stand up the **backend host** of the AI Ops Manager: the `:aiops` module running the **same host-agnostic
engine SPI** (from Phase A) with **Koog** as the reasoning port and other modules' **public services** as
read/write adapters. Prove it with **one cross-entity capability — customer dedup** (reversible LINK) —
plus the nightly scan runner and unified audit.

## 2. Scope (Phase B)
**In:**
- `:aiops` module (`com.ampairs.aiops`), `:core` + Koog; business data via public services only.
- Consume the shared engine SPI (framework §3) — ideally the *same* artifact as the app (framework §18.4).
- Backend `Reasoner`=Koog, `Executor`=owning public service, `AuditWriter`/`FeedbackStore`=Postgres.
- Four `aiops_*` tables (Postgres, Flyway both vendors, `migrationModules`); **reconcile app-synced
  findings/audit** so history is unified (framework §6).
- Event-driven + **nightly per-workspace scan**, each inside a tenant scope carried across Koog threads
  (framework §7); engine-level **two-tenant test mandatory**.
- Review-queue + audit + **rollback** APIs under `/aiops/v1`; orchestrator → morning-brief summary.
- **One capability: customer dedup** (name+phone+GSTIN+address ensemble → reversible LINK).

**Out:** single-entity/implicit app capabilities (Phase A); inventory/purchase/sales anomalies (Phase C);
L4 fully-autonomous defaults; RAG store (framework §18.5).

## 3. Hard constraints
Bounded context + writes via owning public services (rule 08); tenant scope per workspace + two-tenant
test (rules 05/06); LLM one capped signal, tax/HSN advisory-only (framework §4/§8); `ApiResponse<T>`, no
controller try/catch (rule 04); DTOs in `aiops/domain/dto/` (rule 02); SNAKE_CASE (03); `Instant` (01);
secrets from env (10); every mutation audited + reversible; default autonomy L1, auto-fix opt-in.

## 4. Success criteria
- Nightly scan dedups customers across the full workspace set, LINKing a 99%+ ensemble match and
  queuing an ambiguous one; a human verdict writes feedback + audit; `revert` unlinks via `CustomerService`.
- Two-tenant scan test green (no cross-tenant leakage).
- App-applied Phase-A audit rows appear in the unified backend audit.
- `./gradlew :aiops:compileKotlin :aiops:test` green; existing modules untouched.

## 5. Open questions
Framework §18.3 (async/job model), §18.4 (shared-engine packaging — is the KMP `aiops-engine` published
so backend reuses it?), §18.5–7. Resolve before `/speckit.plan`.

---

_Next step: after Phase A ships and §18.3–4 are decided, `/speckit.specify` to expand §2–§4._
