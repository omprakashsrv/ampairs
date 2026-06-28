# Offline-Sync Endpoint Contract

The mobile app (Compose Multiplatform, separate repo) is offline-first: every workspace entity is
mirrored in a local Room DB and reconciled with the backend through **one unified REST contract**.
Every standard syncable resource exposes the **same pair of endpoints** so the client's generic sync
engine (`CentralSyncService` + one `SyncDelegate` per entity) can drive them all identically.

When you add or change a syncable resource, conform to this contract exactly.

---

## The canonical contract

For a resource at module base `/{module}/v1/{resource}`:

```
PULL   GET  /{module}/v1/{resource}/sync
            ?last_sync={ISO-8601}&page={int}&size={int}&sort_by=updatedAt&sort_dir=ASC
       → ApiResponse<PageResponse<{Resource}Response>>

PUSH   POST /{module}/v1/{resource}/sync          (same URL as pull)
       body: List<{Resource}Request>   (client UID-keyed; active upserts AND soft-deletes)
       → ApiResponse<List<{Resource}Response>>
```

### Rules

1. **Same URL, `/sync` suffix** for both GET (pull) and POST (push). No separate non-`/sync` list or
   bulk endpoints — they were removed.
2. **Query params are `snake_case`**: `last_sync`, `page`, `size`, `sort_by`, `sort_dir`.
   - `last_sync` is optional (ISO-8601 `Instant`, URL-decoded then `Instant.parse`). When absent/blank,
     return the full feed.
   - Defaults: `page=0`, `size=100`, `sort_by=updatedAt`, `sort_dir=ASC`.
3. **The pull feed MUST include soft-deleted rows** (`status = DELETED` / `active = false`). This is how
   deletions propagate to every device. Do **not** filter them out — the sync query is intentionally
   different from a normal user-facing list.
4. **Deletes are in-band.** There is **no** per-row `DELETE` endpoint in the sync path. The client sends
   soft-deleted rows inside the push body; the server upserts them with the delete flag set. On pull,
   the client permanently hard-deletes any row the server reports as deleted.
5. **Push is a UID-keyed bulk upsert.** For each item: if `uid` exists → update, else → create. Honor the
   soft-delete flag. The client batches at 100; the server must accept a `List`. Return the
   server-resolved rows (the client reconciles by `uid`).
6. **Wrappers**: pull → `ApiResponse<PageResponse<T>>` (via `PageResponse.from(page)`); push →
   `ApiResponse<List<T>>`. DTO isolation applies — never return JPA entities.
7. **Tenant context** is set at the controller level (`@TenantId` auto-filters the workspace); the sync
   query and upsert are workspace-scoped automatically.

---

## Controller skeleton

```kotlin
@RestController
@RequestMapping("/{module}/v1/{resource}")
class XController(private val service: XService) {

    @GetMapping("/sync")
    fun sync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<XResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy))
        return ApiResponse.success(PageResponse.from(service.getAfterSync(lastSync, pageable)) { it.asResponse() })
    }

    @PostMapping("/sync")
    fun push(@RequestBody @Valid requests: List<XRequest>): ApiResponse<List<XResponse>> =
        ApiResponse.success(service.bulkUpsert(requests))
}
```

## Service / repository pattern

```kotlin
// Sync feed INCLUDES soft-deleted rows — no status/active filter.
@Query("SELECT x FROM x x WHERE x.updatedAt >= :lastSync")
fun findUpdatedAfter(lastSync: Instant, pageable: Pageable): Page<X>

@Query("SELECT x FROM x x")          // full feed when no last_sync; still includes inactive
fun findAllForSync(pageable: Pageable): Page<X>

fun getAfterSync(lastSync: String?, pageable: Pageable): Page<X> =
    if (lastSync.isNullOrBlank()) repo.findAllForSync(pageable)
    else runCatching { repo.findUpdatedAfter(Instant.parse(URLDecoder.decode(lastSync, UTF_8)), pageable) }
        .getOrElse { repo.findAllForSync(pageable) }

@Transactional
fun bulkUpsert(requests: List<XRequest>): List<XResponse> = requests.map { req ->
    val existing = req.uid?.takeIf { it.isNotBlank() }?.let { repo.findByUid(it) }
    (existing?.applyRequest(req) ?: req.toEntity()).let(repo::save).asResponse()
}
```

---

## Resources on the contract

`customer`, `customer_group` (`/groups/sync`), `customer_type` (`/types/sync`), `product`,
`product` catalog (`/groups/sync`, `/categories/sync`, `/brands/sync`, `/sub-categories/sync`),
`unit`, `setting` (store), `order`, `invoice`, `price_list` (`/pricing/v1/price-lists/sync`),
`price_list_item` (`/pricing/v1/price-lists/items/sync`), `geo_zone` (`/pricing/v1/geo-zones/sync`).

> **Pricing money is minor-unit asymmetric** (spec 009): price-list-item PULL returns
> `unit_price` as a `MoneyDto { amount_minor, currency }`; PUSH sends `unit_price_minor: Long`
> (currency inherited from the parent list — single-currency-per-list).

## Aggregate-grained on the contract

- **form** — a single feed `GET/POST /form/v1/config/schema/sync` carries **one `FormSchema`
  aggregate per entityType** (uid = entityType; each aggregate bundles its ordered sections + fields).
  It is on the canonical `/sync` contract with two aggregate-specific nuances:
  - **Delete-by-absence**: there is no per-row soft-delete; a section/field omitted from a pushed
    `FormSchema` is deleted server-side and disappears on the next pull on every device.
  - **Optimistic concurrency**: the push carries `base_version`; a stale push (`base_version` <
    current `version`) is rejected, and the client re-pulls, re-applies local edits, and retries
    (aggregate-level last-write-wins).

## Intentionally **off** the contract

- **tax** — the server mints the workspace tax code; writes are `POST /codes/subscribe`,
  `/bulk-subscribe`, and `DELETE /codes/{id}` (unsubscribe). A subscription model, not client-authored
  UID-keyed rows, so a bulk `/sync` push does not apply.
- **file** — binary image upload via multipart `POST /images/{type}/{uid}`, entity-scoped `GET`, per-image
  `DELETE`. Binary can't ride a JSON `List<T>` body, and it's UI-invoked, not central-sync.

---

## Checklist — adding a new syncable resource

- [ ] `GET /sync` with `last_sync, page, size, sort_by, sort_dir` (snake_case), `ApiResponse<PageResponse<T>>`.
- [ ] Sync query **includes soft-deleted rows** (no `status`/`active` filter).
- [ ] `POST /sync` accepting `List<Request>`, UID-keyed bulk upsert honoring the soft-delete flag,
      `ApiResponse<List<T>>`.
- [ ] No per-row `DELETE` in the sync path (deletes are in-band).
- [ ] Entity has `updatedAt: Instant` + a soft-delete flag; if not, add a Flyway migration first.
- [ ] DTO isolation (Request/Response in `domain/dto/`); `@TenantId` workspace scoping.
- [ ] Mirror the client `SyncDelegate` on the app side (see app repo `/offline-sync` skill).
