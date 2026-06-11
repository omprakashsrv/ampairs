# Migration housekeeping log

Per `.claude/rules/07-migrations.md`, schema-adjacent changes that intentionally ship without a
new migration (or that remove migration artifacts) are documented here.

## 2026-06-10 — removed ad-hoc repo-root SQL scripts

- **`fix-flyway-checksum.sql` (deleted).** One-time manual repair of the `flyway_schema_history`
  checksum for V1.0.38 after that applied migration was edited in place (a violation of the
  "never modify an applied migration" rule). The repair was already executed against production
  per the script's own instructions. Any environment still hitting the
  "Migration checksum mismatch for migration version 1.0.38" error should run the supported
  tooling instead: `./gradlew :ampairs_service:dbRepair`.

- **`fix-tax-rule-code-ids.sql` (deleted, converted to a migration).** The data repair
  (backfilling empty `tax_rule.tax_code_id` from the workspace's `tax_code` rows) now ships as
  `tax/src/main/resources/db/migration/{postgresql,mysql}/V1.0.81__backfill_tax_rule_tax_code_id.sql`
  so it runs once, everywhere, under Flyway's control. (Originally authored as V1.0.80, renumbered
  to V1.0.81 to avoid colliding with `form/.../V1.0.80__create_unified_form_model.sql`, which
  landed on main in parallel.) The diagnostic SELECTs from the old script were dropped; only the
  idempotent UPDATE was kept.

- **`workspace/src/main/resources/db/migration/V2.0.0__add_retail_modules.sql` (deleted, dead file).**
  It sat *outside* the vendor directories, and every Flyway location in use
  (`classpath:db/migration/{vendor}` in Spring, `.../db/migration/postgresql` in the Gradle
  tasks) only scans vendor subdirectories — so this migration has never executed anywhere.
  Its content (seeding `master_modules` rows for product/order/etc. modules) is fully superseded
  by the code-based `workspace/.../service/MasterModuleSeederService.kt`, which is the single
  source of truth for the master module catalog. Resurrecting the SQL would have double-seeded
  rows the seeder already manages.

## 2026-06-10 — MySQL vendor parity restored

The MySQL migration history had drifted ~15 files behind PostgreSQL (and Flyway could not even
run against MySQL — `flyway-mysql` was not on the classpath). Restored:

- Added `org.flywaydb:flyway-mysql` to `ampairs_service` (runtime + the Gradle `flywayRuntime`
  configuration) and made the `db*` Gradle tasks vendor-aware (the JDBC URL now selects the
  `mysql/` or `postgresql/` location, mirroring Spring's `{vendor}` placeholder).
- Authored MySQL counterparts for: ecom V1.0.62–66, 67 (as FULLTEXT), 69, 70, 73, 74;
  auth V1.0.68; user V1.0.59; order V1.0.61; unit V1.0.52–53; product V1.0.51
  (includes `warehouse.address_phone`) and V1.0.60.

**Intentionally NOT mirrored to MySQL (no-ops there):**

- `V1.0.54/55/56/57/58__fix_*_timestamp_types.sql` — PostgreSQL-only TIMESTAMP→TIMESTAMPTZ
  conversions. MySQL `TIMESTAMP` already normalizes to UTC; there is nothing to change.
- `product V1.0.72__make_product_sku_optional` — the same change shipped on MySQL as
  `V1.0.50__make_product_sku_optional` (vendor histories number it differently; both applied).
- `tax V1.0.9` (mysql) vs `tax V1.0.10` (postgresql) — identical "create tax module tables"
  content under different version numbers; both vendors are covered.

**Known code-level gaps for a MySQL deployment (schema is ready, queries are not):**

- `ecom/.../EcomListedProductRepository` search uses PostgreSQL `search_vector @@
  plainto_tsquery(...)`. MySQL got a FULLTEXT index (V1.0.67) but needs a
  `MATCH ... AGAINST` query variant.
- `product/.../InventoryTransactionRepository` (`findBySerialNumber`) uses
  `serial_numbers::jsonb @> jsonb_build_array(...)`; MySQL needs `JSON_CONTAINS`.

PostgreSQL remains the primary/production vendor; MySQL migrations are currently authored by
translation and are not yet exercised by CI (no MySQL integration tests).
