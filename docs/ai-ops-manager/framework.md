# AI Business Operations Manager — Framework Blueprint

**Status:** Draft blueprint (architecture, not yet a build commitment)
**Proposed module:** `:aiops` — new bounded context `com.ampairs.aiops`
**Reasoning layer:** Koog (see `.claude/skills/koog/SKILL.md`)
**Related:** `docs/spikes/koog-agent-backend-spike.md` · `specs/030-koog-agent-assistant` (read-only Q&A precursor) · `specs/031-ai-ops-manager-foundation` (foundation slice) · PR #221

> **How to read this.** This is the framework that the product vision ("AI Retail Business Operations
> Manager") maps onto in the Ampairs backend. It defines the reusable **decision engine** and the
> **agent/pipeline SPI** so every future capability (product dedup, customer dedup, HSN check, contact
> card → customer, inventory/purchase/sales anomalies) is *the same machine with different plug-ins*,
> not a pile of one-off scripts. Concrete features are specced separately via speckit and hang off this
> doc. Nothing here ships until a foundation spec is planned and approved.

---

## 1. The one principle everything serves

> **Automate what is predictable. Escalate what is uncertain. Learn from every human decision. Log and
> reverse everything.**

Operationally that means every change the system makes is gated on **two independent axes** —
**confidence** (how sure are we of the correct value) and **risk** (how much damage if we're wrong) —
and the LLM is **never the sole authority**. Auto-fix happens only when confidence is high *and* risk is
low enough for the workspace's configured autonomy level. Everything else becomes a human exception.

This is the same confidence-gate the Koog spike de-risks, generalized into a reusable engine.

---

## 2. Where it lives — a new bounded context, `:aiops`

A new module (bounded-context rule 08), **not** bolted onto `:agent`:

| Module | Role |
|---|---|
| `:agent` (exists) | App-facing: on-device model manifest/download proxy, cloud chat completions, chat telemetry. **Unchanged.** |
| **`:aiops` (new)** | The autonomous back-office manager: detectors, decision engine, agents, review queue, audit, learning. Depends on `:core` + Koog; reaches business data through **other modules' public service interfaces only**. |

**Dependency rules (non-negotiable):**
- `:aiops` **reads** business data via public services (`ProductService`, `CustomerService`,
  `UnitService`/`UnitConversionService`, tax services, inventory services) — never their repositories.
- `:aiops` **writes corrections** *only* by calling the owning module's public write service (e.g. a
  product-name fix goes through `ProductService.update…`, so that module's validation/sync/`@TenantId`
  rules all still apply). `:aiops` never mutates another module's tables directly.
- `:aiops` owns **only its own tables** (findings, decisions/audit, feedback, agent runs).
- Koog stays confined to `:aiops` (and `:agent`), per the Koog skill.

This keeps the manager a *coordinator over the existing domain*, not a shadow copy of it.

---

## 3. The framework SPI — one pipeline, many plug-ins

The product vision's pipeline (Detect → Understand → Context → Candidates → Validate → Confidence →
Risk → Auto/Ask → Audit → Learn) becomes a fixed **engine** with pluggable **stages**. A "capability"
(e.g. *unit standardization*, *customer dedup*) = one implementation set of these interfaces registered
under a key. Adding a capability adds plug-ins; it never edits the engine.

```kotlin
package com.ampairs.aiops.engine

// A problem the system noticed. entityType/entityId point at the owning module's aggregate.
data class Finding(
    val id: String,               // UID
    val capability: String,       // "product.unit", "customer.duplicate", "product.hsn", ...
    val entityType: String,       // "product" | "customer" | ...
    val entityId: String,         // the aggregate uid in the owning module
    val field: String?,           // affected field, if a single-field issue
    val summary: String,          // human-readable "what's wrong"
    val signals: Map<String, String>, // raw evidence the detector saw
)

// A proposed fix. before/after drive both apply and rollback.
data class Candidate(
    val field: String?,
    val before: String?,
    val after: String?,
    val action: ActionType,       // UPDATE_FIELD | MERGE | LINK | DEACTIVATE | CREATE | ...
    val rationale: String,        // why (evidence-based, not "the LLM said so")
    val evidence: List<String>,   // deterministic facts that support it
)

enum class ActionType { UPDATE_FIELD, MERGE, LINK, DEACTIVATE, CREATE, SPLIT, NO_OP }

// ── The five pluggable stages (one set per capability) ─────────────────────────
fun interface Detector {          // 1. find problems (deterministic queries + heuristics)
    suspend fun detect(scope: WorkspaceScope): List<Finding>
}
fun interface ContextGatherer {   // 2. enrich a finding with the facts the scorers need
    suspend fun gather(finding: Finding): FindingContext
}
fun interface CandidateGenerator {// 3. propose fixes (deterministic + LLM via Koog)
    suspend fun propose(finding: Finding, ctx: FindingContext): List<Candidate>
}
fun interface CandidateValidator {// 4. reject impossible/unsafe candidates against DB + rules
    suspend fun validate(finding: Finding, candidate: Candidate, ctx: FindingContext): Validation
}
fun interface ConfidenceScorer {  // 5. ENSEMBLE score — see §4 (never LLM alone)
    suspend fun score(finding: Finding, candidate: Candidate, ctx: FindingContext): Confidence
}
```

The engine wires them: `Detector → ContextGatherer → CandidateGenerator → CandidateValidator →
ConfidenceScorer → RiskPolicy → (Executor | ReviewQueue) → AuditWriter → FeedbackStore`. Only the
stages are capability-specific; the flow, gating, audit, and learning are shared.

---

## 4. The decision engine — confidence × risk (the heart)

### Confidence is an ensemble, never the LLM alone
`ConfidenceScorer` combines weighted signals; the LLM is one input, capped so it can't dominate a
high-risk change:

- **Deterministic rules** — exact/normalized equality, format validators (GSTIN checksum, phone,
  pincode↔city↔state reference tables), unit alias tables.
- **Statistical similarity** — string distance (Jaro-Winkler/token-set) for names, blocking keys for
  dedup, historical frequency ("same SKU always uses ML").
- **Reference/external data** — HSN master, pincode master, GSTIN registry format.
- **LLM reasoning (Koog)** — semantic judgment: "these 4 names are the same drink", "this hierarchy is
  inverted", "this HSN looks wrong for a soft drink". Returned as **structured output** with the model's
  own self-confidence and rationale (see §8).

The scorer emits `Confidence(value: Double, band: HIGH|MEDIUM|LOW, contributors: Map<source,score>)`.
Contributors are stored in the audit so a decision is fully explainable.

### Risk is a property of the field/action, not the model
`RiskPolicy` classifies each candidate: reversibility, blast radius (single field vs. merge of two
customers with transactions), and **field sensitivity**. Tax/HSN/GST and anything money- or
compliance-adjacent is **high risk by policy** and gets a stricter ceiling — *the vision's "don't blindly
modify tax data on an LLM prediction" is encoded here as a hard rule, not a guideline.*

### The gate
```
autoFix  ⇔  confidence.value ≥ threshold(capability, autonomyLevel)
            AND risk ≤ ceiling(capability, autonomyLevel)
            AND candidate is reversible (or explicitly allowed irreversible)
otherwise ⇒ enqueue as a human exception (REVIEW or ASK)
```
Thresholds/ceilings are per-capability and per-autonomy-level (§5), workspace-configurable.

---

## 5. Autonomy levels (per workspace)

The vision's L0–L4 become a workspace setting (stored via the existing `setting` module,
`SettingService`), read by the gate:

| Level | Behavior | Gate effect |
|---|---|---|
| 0 Observe | detect only | never auto-fix; everything → findings dashboard |
| 1 Recommend | propose | never auto-fix; everything → review queue as suggestions |
| 2 Auto-correct | fix low-risk/high-confidence fields | auto-fix where gate passes on **low-risk** capabilities only |
| 3 Auto-execute | routine ops per policy | wider capability set eligible; money/tax still escalate |
| 4 Autonomous | continuous, escalate only real judgment calls | broadest eligibility; hard-risk ceilings still hold |

Default **Level 1** for a new workspace (safe: proposes, never changes). Owners opt up. Tax/HSN never
auto-applies below an explicit per-workspace opt-in regardless of level.

---

## 6. Data model (`:aiops` tables)

All entities extend `OwnableBaseDomain` (workspace-scoped, `@TenantId`), all timestamps `Instant`,
Flyway migrations under **both** `postgresql/` and `mysql/`, module added to `migrationModules`.

- **`aiops_finding`** — one detected issue. `capability, entity_type, entity_id, field, status
  (OPEN|AUTO_FIXED|PENDING_REVIEW|ACCEPTED|REJECTED|IGNORED), summary, signals(jsonb), created_at`.
- **`aiops_decision`** (the audit trail) — one row per action taken/proposed. Stores the vision's full
  audit tuple: `finding_id, entity_type, entity_id, field, before, after, action, confidence,
  confidence_contributors(jsonb), risk_level, reason, evidence(jsonb), source(AUTO|HUMAN), agent,
  human_approval(nullable), reversible(bool), reverted_at(nullable), created_at`.
- **`aiops_feedback`** — human verdicts used for learning: `finding_id, candidate_ref, verdict
  (APPROVE|REJECT|EDIT|MERGE|KEEP_BOTH|IGNORE), edited_value, decided_by, created_at`.
- **`aiops_agent_run`** — one scan/execution: `trigger(EVENT|SCHEDULE|USER), capability, scope,
  counts(reviewed/auto_fixed/needs_review/failed), started_at, finished_at, status`.

`before`/`after` on `aiops_decision` are what make **rollback** a first-class operation (§11).

---

## 7. Execution model

Three triggers, one engine:

- **Event-driven** — on entity create/update, run the relevant detectors for that one aggregate. Hook
  the existing `event` module (workspace event stream / Kafka consumer) rather than inventing a bus.
  Example: new product created → `product.*` detectors → fix/flag.
- **Scheduled** — nightly full scan per workspace: detect → prioritize → auto-resolve → build the
  exception list that feeds the morning brief (§9/§14 of the vision).
- **User-triggered** — "Clean my product database" → run a capability (or all) synchronously-ish with a
  progress/report response.

### Tenant propagation at scale (the sharp edge, same as the spike, ×N workspaces)
Background scans have **no request thread**, so there's no controller to set `TenantContextHolder`. The
engine must, for each workspace it processes, establish tenant context around all reads/writes — and,
because Koog runs tool/LLM calls on switching coroutine threads, carry it with a
`ThreadContextElement` (the mechanism the spike validates) so the owning modules' `@TenantId` services
keep working unchanged. **Every detector/executor runs inside a per-workspace tenant scope.** This is the
#1 correctness risk of the whole framework and gets a mandatory multi-tenant test at the engine level.

---

## 8. Koog's role — reasoning, never authority

Koog powers the *judgment* stages only; deterministic code owns the rest.

- **CandidateGenerator / semantic scoring** — Koog `AIAgent`/structured output for: duplicate
  probability across fuzzy names, hierarchy-inversion proposals (Category/Brand/Sub-category),
  HSN/GST plausibility, unit/quantity/pack-size parsing of messy strings.
- **Tools** — read-only tools over Ampairs data (similar products, historical SKU usage, existing
  customers by phone/GSTIN) so the model reasons over *real* business facts, not just the prompt.
- **Structured output** — every LLM call returns a typed verdict `{ candidates[], selfConfidence,
  rationale, evidenceRefs }` (Koog structured output + auto-repair). That feeds the ensemble as **one
  weighted contributor**, capped by `RiskPolicy`.
- **Contact card → customer (§12 of vision)** — Koog with a vision-capable model: OCR/extract → normalize
  → match against existing customers (tools) → create/link or escalate. Same engine, `entityType =
  "customer"`, `action = CREATE|LINK`.

Guardrail: a capability may be configured **LLM-advisory-only** (LLM can lower confidence / add
rationale but cannot by itself push a candidate over the auto-fix line) — mandatory for tax/HSN.

---

## 9. Agent catalog (maps vision §15 to `:aiops` capabilities)

Each "agent" is a grouping of capabilities over one module; all share the engine.

| Agent | Phase-1 capabilities | Later |
|---|---|---|
| **Product** | dedup, unit standardization, hierarchy (brand/category/subcat), HSN/GST sanity (advisory) | SKU/barcode validation, enrichment, lifecycle |
| **Customer** | dedup (name+phone+GSTIN+address ensemble), field validation (GSTIN/phone/pincode↔city↔state), **contact card → customer** | segmentation, inactivity |
| **Inventory** | — | low/over/dead-stock, reconciliation anomalies |
| **Purchase** | — | supplier price anomalies, duplicate entries |
| **Sales** | — | discount/pricing anomalies, duplicate invoices |

A thin **orchestrator** schedules agents, aggregates their `aiops_agent_run` results into one workspace
summary (the morning brief), and enforces global rate/cost limits.

---

## 10. Human-in-the-loop

Exceptions only. API surface (under `/aiops/v1`, tenant-scoped, `ApiResponse<T>`, DTOs in
`domain/dto/`):
- `GET /aiops/v1/findings` — paged exception queue (filter by capability/entity/band).
- `POST /aiops/v1/findings/{id}/decision` — `APPROVE | REJECT | EDIT | MERGE | KEEP_BOTH | IGNORE`,
  plus **`applyToSimilar`** (fan the same verdict to matching findings — vision's "Apply to Similar").
- `POST /aiops/v1/findings/bulk` — "Accept all high-confidence".
- Every human decision writes `aiops_feedback` (→ learning) and an `aiops_decision` audit row.

The mobile app renders this as the manager brief + a decision inbox; the framework only exposes the
data + actions.

---

## 11. Audit & rollback (first-class)

- Every action (auto or human) writes an `aiops_decision` with before/after + full evidence/confidence.
- **Rollback** = `POST /aiops/v1/decisions/{id}/revert`: re-apply `before` through the owning module's
  public write service, stamp `reverted_at`. A capability must declare each action **reversible** or
  explicitly irreversible; irreversible actions (e.g. a hard customer merge) require higher autonomy +
  human approval and may be modeled as reversible **LINK** instead of destructive MERGE where possible.
- The owner can always answer: *what changed, why, and can I undo it* — straight from `aiops_decision`.

---

## 12. Learning loop

Human verdicts in `aiops_feedback` improve future decisions without retraining a model:
- **Threshold tuning** — per capability, track precision/false-positive from approvals vs. rejections;
  nudge auto-fix thresholds within owner-set bounds.
- **Few-shot memory** — store representative approved/rejected pairs as Koog prompt examples for that
  capability (retrieval-scoped per workspace).
- **Rule promotion** — a fuzzy pattern humans always approve (e.g. a specific unit alias) graduates into
  a deterministic rule, moving it out of the LLM path entirely.

Start with threshold tuning + few-shot (cheap, safe); rule promotion is a later phase.

---

## 13. Safety & guardrails (checklist the engine enforces)
- [ ] Writes only via owning-module public services (never cross-module repo writes).
- [ ] Every workspace scan runs in a per-workspace tenant scope carried across Koog threads (§7).
- [ ] Confidence is an ensemble; LLM contribution capped; tax/HSN LLM-advisory-only.
- [ ] Auto-fix gated on confidence **and** risk **and** reversibility (§4), per autonomy level (§5).
- [ ] Dry-run mode for every capability (produce candidates + would-be actions, apply nothing).
- [ ] Idempotent actions (re-running a scan can't double-apply) and per-workspace cost/rate limits.
- [ ] Full audit + rollback for every mutation.
- [ ] Default autonomy = Recommend (L1); auto-fix is opt-in per capability.

---

## 14. Success metrics (vision §19 → concrete)

Emit from `aiops_agent_run` + `aiops_decision` + `aiops_feedback`:
- **Auto-resolution rate** = AUTO_FIXED / total findings; **human-intervention rate** = its complement.
- **AI precision** = approved / (approved + rejected) among reviewed; **false-positive rate** and
  **rollback rate** = reverted / auto_fixed.
- **Data-quality trend** — duplicate rate, invalid/missing-field rate, standardization rate over time.
- **Primary KPI (vision):** % of operations completed correctly without human intervention =
  auto_fixed_not_reverted / total_actions.

---

## 15. Phasing & how it decomposes into speckit specs

- **Phase 0 — Foundation** (`specs/031-ai-ops-manager-foundation`): the `:aiops` module, the engine SPI,
  the four tables, autonomy setting, review-queue + audit + rollback APIs, tenant-scoped scan runner,
  **one trivial capability end-to-end** (unit standardization — highest confidence, lowest risk) as the
  proof the machine works. Depends on the Koog spike's tenant-propagation result.
- **Phase 1 — Data quality** (own specs): product dedup, product hierarchy, customer dedup + field
  validation, **contact card → customer**, HSN/GST advisory.
- **Phase 2 — Operations** (own specs): inventory, purchase, sales, pricing, supplier anomalies; the
  morning-brief orchestration; higher autonomy levels.

Each phase-1/2 capability is *just another plug-in set* against the Phase-0 engine.

---

## 16. Open decisions (resolve before/at Phase-0 speckit)
1. **Module name** — `:aiops` vs `:opsmanager` vs `:dataquality` (this doc assumes `:aiops`).
2. **Async model** — background scans need a job runner (Spring `@Scheduled` + a work queue? Kafka via
   the `event` module? external scheduler?). Blocking `runBlocking` is fine for user-triggered, not for
   fleet-wide nightly scans.
3. **Vector/RAG store** for few-shot memory and dedup blocking — needed in Phase 1? Which store?
4. **Merge semantics** — do we ever hard-merge customers/products, or only LINK + supersede? (Strong
   preference for reversible LINK.)
5. **Cost budget** — per-workspace LLM spend ceiling and model tiering (cheap model for scoring, larger
   only on escalated/ambiguous cases).
6. **Reference data** — source/refresh for HSN master, pincode↔city↔state, GSTIN format tables.

---

_Next step: approve the module name + async model (§16.1–2), then run `/speckit.specify` against
`specs/031-ai-ops-manager-foundation` to turn Phase 0 into an implementable spec._
