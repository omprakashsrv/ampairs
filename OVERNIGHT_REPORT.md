# Overnight work report — 2026-06-10

Autonomous session. Everything below is committed and pushed; nothing awaits local steps from you
except review. Backend changes were each validated by compiling + running tests here. App changes
are additive/config-only and **could not be machine-compiled in the sandbox** (the KMP build tries
to download a JetBrains JDK toolchain and the network blocks it) — they're validated by the new app
PR workflow instead.

- Backend PR: **omprakashsrv/ampairs#103** — branch `claude/nice-planck-gwivv0`
- App PR: **omprakashsrv/ampairs-app#47** — branch `claude/nice-planck-gwivv0`
- Backend full test suite at end of night: **186 passing, 0 failing** (was 154).

## What landed (backend `ampairs`)

| Commit | What |
|--------|------|
| `b9629bf` | **Security hardening (P0):** OTP hardcoded-bypass now secure-by-default in prod; reCAPTCHA on `/verify/firebase` is a real configurable check (not commented out); payment providers with placeholder secrets aren't registered; fixed `/refresh-token` rate-limit key mismatch. |
| `5ad4b04` | **Migrations (P0):** restored MySQL vendor parity (added `flyway-mysql`, vendor-aware Gradle tasks, ~15 authored MySQL migrations); converted the two root `fix-*.sql` scripts into a forward migration; removed the dead `V2.0.0` file. |
| `40e3232` | **Migration collision fix:** merged `origin/main` (brought in the form module + its `V1.0.80`) and renumbered the tax backfill to `V1.0.81`. This is what unblocked your Flyway checksum-mismatch startup error. |
| `1f50710` | **CI + the #1 missing test:** `pr.yml` runs the suite on every PR and applies all migrations to a fresh PostgreSQL + validates them (would have caught the collision pre-merge). `TenantIsolationIntegrationTest` proves Hibernate `@TenantId` actually isolates tenants. |
| `67ad371` | **Order money-math tests** — pins the arithmetic; includes a regression guard documenting a **likely double-counting of item discounts** (see "Needs your decision"). |
| `7f56fa0` | **Ops:** `server.shutdown=graceful` (+25s drain), CODEOWNERS, Dependabot. |
| `513c3a5` | **ADRs** (`docs/adr/`) for multi-tenancy, the `/sync` contract, Flyway vendor policy, and the OTP/reCAPTCHA hardening. |
| `8fe96f5` | Test for the new `OtpSecurityStartupCheck` prod guard. |

## What landed (app `ampairs-app`)

| Commit | What |
|--------|------|
| `f5d257e` | Untracked `google-services.json` (+ template, CI injection); aligned `ProductSyncDelegate` to the resilient Customer push pattern. |
| `9aef8c0` | `pr.yml` compiling Android+Desktop+iOS and running tests on every PR; expanded R8 keep rules (kotlinx.serialization, Ktor, Room, Metro, Nav3) to prevent release-only crashes; CODEOWNERS + Dependabot. |

## Needs your decision (no action taken)

1. **Order item-discount double-count.** `Order.calculateTotals()` subtracts each item's discount
   twice — once inside `OrderItem.calculateLineTotal()` (which nets it into `lineTotal`) and again
   via the `itemDiscounts` term. A $100 item with a $10 line discount yields `totalAmount = 80`, not
   90. Pinned by `OrderCalculationTest.item discount is currently applied twice`. If that's a bug, the
   fix is one line in `calculateTotals()`; I left billing logic alone deliberately.
2. **reCAPTCHA on Firebase verify** ships **disabled** (`RECAPTCHA_ENFORCE_FIREBASE=false`) because
   current clients send no token. Enable it only after the client sends one, or Firebase login breaks.
3. **OTP secure defaults change prod behavior:** if any prod deploy was silently relying on
   `OTP_DEV_MODE`/hardcoded OTP, it must now set the env vars explicitly (and
   `OTP_ALLOW_HARDCODED_IN_PRODUCTION=true` to keep the bypass) — otherwise the app fails fast on boot.
4. **New CI secret:** both `pr.yml` workflows and the app release workflow expect a
   `GOOGLE_SERVICES_JSON` repo secret (app) — set it so Android CI can build.

## Deliberately NOT done (and why)

- **detekt/ktlint static analysis** — skipped to avoid leaving the build broken unattended on the
  bleeding-edge Kotlin 2.3.20 / Gradle 9.4 stack. Worth adding interactively (P1-5).
- **App convention plugins** (de-dup 26 build files) and **BaseSyncDelegate / SyncController**
  abstractions — high-value but high-risk refactors I won't do blind without being able to compile
  the app. (P1-3.)
- **Cross-module repository-coupling refactor** (ecom→other repos) and an ArchUnit boundary test —
  the test would fail on existing violations; needs the refactor first. (P1-4.)
- **App branch was NOT merged with app/main** (unlike backend) — I can't compile to resolve conflicts
  safely. If app `#47` CI shows it's behind, merge main locally.
- **MySQL is schema-ready but not query-ready:** two repos use Postgres-only SQL (ecom FTS, inventory
  jsonb). Tracked in `NO_MIGRATION_NEEDED.md`. Postgres remains primary/production.

## Suggested next session (priority order)

1. Decide the item-discount double-count (#1 above) and set the `GOOGLE_SERVICES_JSON` secret.
2. Add detekt + ktlint with a baseline, wired into both `pr.yml`s.
3. Extract `BaseSyncDelegate<T>` (app) and a `SyncController<T>` (backend) to kill the per-entity
   copy-paste; add app convention plugins.
4. Refactor ecom cross-module repo access behind service interfaces; add an ArchUnit boundary test.
