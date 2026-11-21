# AI Business Operations Manager — Program Roadmap

**This is the single entry point.** It maps every artifact, states the architecture in one line, and
gives the build order + the method for planning each slice. Start here; follow the links for depth.

> **North-star.** An AI that continuously reviews a workspace's operational data (products, customers,
> units, HSN/tax, later inventory/purchase/sales), **auto-fixes what is predictable**, **escalates what
> is uncertain**, **learns from every human decision**, and **logs & can reverse everything** — so the
> owner manages exceptions, not spreadsheets.

> **Architecture in one line (ADR 0005).** The orchestrator runs **app-side** (one workspace per
> instance, full master resident in Room, existing `LlmEngine` + cloud tier, audited write→sync path).
> The **backend** (`:aiops` + Koog) is a **deferred, optional tier** for always-on autonomy and
> admin cross-tenant only.

---

## Artifact map (what exists and its status)

| Artifact | What it is | Status |
|---|---|---|
| **This roadmap** (`docs/ai-ops-manager/README.md`) | Program spine, backlog, method | active |
| [framework.md](framework.md) | The engine design (SPI, decision engine, data model, walkthroughs) | active (rev 3) |
| [ADR 0005](../adr/0005-ai-ops-manager-app-side-orchestration.md) | Decision: app-side orchestration | accepted |
| [ADR 0006](../adr/0006-aiops-engine-shared-kmp-module.md) | Decision: host-agnostic shared KMP engine | accepted |
| `ampairs-app/docs/design/ai-ops-manager/` | **App-side (Epic 1) design docs** — the near-term build | active |
| [Koog spike](../spikes/koog-agent-backend-spike.md) | Backend tool-calling spike | precursor · deferred |
| [`.claude/skills/koog`](../../.claude/skills/koog/SKILL.md) | Koog reference (backend tier only) | reference |
| [spec 030](../../specs/030-koog-agent-assistant/spec.md) | Backend read-only Q&A assistant | precursor · optional |
| [spec 031](../../specs/031-ai-ops-manager-foundation/spec.md) | Backend `:aiops` (Epic 2) | deferred · optional |

**Reading order for a newcomer:** this roadmap → `framework.md` → ADR 0005/0006 → the app-side design
docs. The backend items are only relevant once Epic 2 is unparked.

---

## Program → Epics → Features

```
AI Business Operations Manager
├── Epic 1 · Data-quality engine + capabilities        [APP · near-term]   ← we build this
│     shared KMP engine · unit standardization · capitalization/format ·
│     customer field validation · contact card→customer · product & customer
│     dedup (resident master) · HSN/GST advisory-suggest
├── Epic 2 · Always-on autonomy + admin                [BACKEND · deferred/optional]
│     overnight scans · morning brief · cross-tenant admin — same engine SPI under Koog
│     (specs 030 Q&A, 031 :aiops foundation, Koog spike live here)
└── Epic 3 · Operations anomalies                      [LATER]
      inventory · purchase · sales · pricing · supplier (host chosen per capability shape)
```

Each capability is **one plug-in set** against the shared engine (ADR 0006). The *host* (app vs backend)
is chosen by capability shape and by whether always-on/admin is required — not per-feature taste.

---

## Cross-repo tracking (where each slice lives)

- **This roadmap + `framework.md` + ADRs** live in **`ampairs`** (backend repo) as the program's source
  of truth.
- **Epic 1 (app) slices** are tracked as **design docs** in
  **`ampairs-app/docs/design/ai-ops-manager/`**, each linked from the backlog below. Code lands in
  `ampairs-app` (`feature/aiops`). The app repo has no speckit; design-doc → review → implement is the
  loop (see Method).
- **Epic 2 (backend) slices** use the **speckit** loop under **`ampairs/specs/###`** (030, 031 already
  reserved).

---

## Sequenced backlog

Ordered; each item names its **home** and **dependencies**. Only 1–5 are near-term.

| # | Slice | Home | Depends on |
|---|---|---|---|
| 1 | **Shared engine SPI + unit standardization** (the proof) | app · `docs/design/ai-ops-manager/01-…` | — |
| 2 | Capitalization/format + customer field validation | app | 1 |
| 3 | Contact card → capture + extract + normalize (vision via cloud tier) | app | 1 |
| 4 | Product & customer **dedup** (resident master; gated on "fully synced") | app | 1 |
| 5 | HSN/GST **advisory-suggest** (never auto) | app | 1 |
| 6 | *[Deferred]* Backend always-on autonomy + morning brief | backend · spec 031 (Koog spike first) | Epic-1 learnings + real need for overnight |
| 7 | *[Later]* Operations anomalies (inventory/purchase/sales/…) | per shape | Epic-1 engine proven |

Item 1 proves the whole machine end-to-end: **detect → candidate → gate → auto-fix → sync → audit →
undo**. Everything after it is another capability on the same rails.

---

## Method — how we plan & build each slice

**App slices (Epic 1)** — Definition of Ready → build → Definition of Done:
1. **DoR:** author a design doc in `ampairs-app/docs/design/ai-ops-manager/NN-<slice>.md` (scope, the
   five stage plug-ins, confidence/risk gate, UX, audit/undo) → review & approve.
2. **Build:** implement in `feature/aiops` following `/cmp-practices`, `/metro-di`, `/offline-sync`.
3. **DoD:** compiles all 3 targets (`androidApp:compileDebugKotlinAndroid`,
   `shared:compileKotlinIosSimulatorArm64`, `desktopApp:compileKotlin`); the capability runs the full
   pipeline; audit + undo work; a test exists; **this roadmap's backlog row is updated**.

**Backend slices (Epic 2, when unparked)** — full speckit loop:
`/speckit.specify → /speckit.clarify → /speckit.plan → /speckit.tasks → /speckit.analyze →
/speckit.implement` under `ampairs/specs/###`. DoD adds the two-tenant scan test + green `:aiops` build.

**Entry conditions to unpark Epic 2:** Epic-1 engine proven on device **and** a concrete need for
always-on/overnight runs or admin cross-tenant visibility. Until then, 030/031/spike stay parked.

---

## Success metrics (carried from framework §14)

From the `aiops_*` audit/feedback records: **auto-resolution rate**, **human-intervention rate**,
**precision** = approved/(approved+rejected), **false-positive** & **rollback rate**, plus
duplicate/invalid/missing-field/standardization trends. **Primary KPI:** % of operations completed
correctly without human intervention = `auto_fixed_not_reverted / total`.

---

## Non-negotiables (apply to every slice, both hosts)
- Confidence is an **ensemble**; the LLM is one **capped** signal, never sole authority. Tax/HSN is
  **advisory-only**.
- Auto-fix only when **confidence high AND risk low AND reversible**, per the workspace's autonomy level
  (default **Recommend / L1** — proposes, never changes).
- Every mutation is **audited and reversible**; corrections flow through the entity's normal write+sync
  (app) or public service (backend) — never a foreign module's storage directly.
