# 0005 — AI Ops Manager: orchestration runs app-side; backend is a deferred tier

- Status: Accepted
- Date: 2026-08-24
- Related: `docs/ai-ops-manager/README.md` (program roadmap), `docs/ai-ops-manager/framework.md`,
  ADR 0006 (shared engine), ADR 0001 (multi-tenancy), ADR 0002 (offline-sync contract)

## Context

We are building an **AI Business Operations Manager** — an agent that continuously reviews a
workspace's master data (products, customers, units, HSN/tax…), fixes high-confidence issues
automatically, and escalates the rest. The open question was **where the orchestrator runs**: the
Compose Multiplatform app (`ampairs-app`) or the Spring backend (`ampairs`, a new `:aiops` module +
Koog).

The facts that decide it:

- The app is **offline-first** and syncs the **whole** customer/product/unit master into local Room
  per workspace (ADR 0002), so a single device already holds the full dataset needed to detect
  duplicates and inconsistencies.
- Each app instance is scoped to **exactly one workspace/user**, so there is no cross-tenant thread
  hazard on device — unlike the backend, where an agent's coroutine threads can drop the thread-bound
  `TenantContextHolder` (ADR 0001) and read the wrong tenant.
- The app already has the **reasoning** it needs: an on-device `LlmEngine` (LiteRT) plus a **cloud
  tier** (`AmpairsProxyTransport` → the backend chat proxy) for hard judgments. Calling the cloud LLM
  is a *reasoning call*, not a backend orchestration hop.
- The app already has an **audited write→sync path**: a correction is a normal offline-first write
  (`synced=false` → `CentralSyncService` → the entity's `SyncDelegate`).

## Decision

**The AI Ops Manager orchestrator runs app-side**, in a shared KMP `feature/aiops` module (ADR 0006).
It detects issues over the resident workspace master, reasons via the app's `LlmEngine` (on-device +
cloud tier), applies fixes through the existing repository + offline-sync path, and records an audit
that syncs to the server.

The **backend** (`:aiops`, Koog, specs `030`/`031`) is a **deferred, optional tier**, justified only by
needs the device cannot meet:
1. **Always-on autonomy** — overnight/continuous scans and the "reviewed your business while you slept"
   brief (a phone is not always on), and
2. **Platform-admin cross-tenant** monitoring across many workspaces.

Even whole-dataset **deduplication runs app-side** over the resident master — it does not require the
backend.

## Consequences

- **Positive:** ships value with **no backend module** and no app release coupling; sidesteps the
  backend tenant-propagation hazard entirely (one instance = one workspace); reuses `LlmEngine`,
  offline-sync, and Metro DI that already exist; the audit reaches the server through normal sync.
- **Negative / risks (recorded, not blockers):**
  - **Non-resident master** — a workspace whose master isn't fully synced onto a device could
    under-detect duplicates. *Mitigation:* gate whole-set capabilities (dedup) on a "master fully
    synced" check; degrade to single-entity capabilities otherwise.
  - **Multi-device duplicate work / write conflicts** — two devices proposing the same fix. *Mitigation:*
    offline-sync's UID-keyed, last-write-wins reconciliation (ADR 0002) absorbs most; prefer reversible
    LINK over destructive MERGE.
- **Backend tier stays designed, not built:** the framework's engine SPI is host-agnostic (ADR 0006) so
  the same capabilities can later run under Koog on the server when (1)/(2) above become real needs.
  Specs `030`/`031` and the Koog spike are parked with explicit entry conditions.
