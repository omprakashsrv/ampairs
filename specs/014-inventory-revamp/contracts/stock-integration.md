# Contract: Order/Invoice → Stock Integration

Defines the cross-module contract by which a confirmed/cancelled sale changes inventory. Honors module
boundaries (Principle IX): order/invoice call a **public service interface** owned by inventory; they never
touch inventory repositories/entities.

---

## Public interface (owned by `com.ampairs.inventory.service`)

```kotlin
interface InventoryStockService {
    /** Apply outbound stock for a confirmed sale. Idempotent per (sourceType, sourceId, line uid). */
    fun applySale(cmd: StockMutationCommand)

    /** Restore stock for a cancellation/return. Idempotent; restores only the given quantities. */
    fun reverseSale(cmd: StockMutationCommand)
}

data class StockMutationCommand(
    val sourceType: SourceType,          // ORDER | INVOICE | RETURN
    val sourceId: String,                // document uid
    val lines: List<StockLine>,
)
data class StockLine(
    val sourceLineUid: String,           // per-line uid → idempotency key
    val productId: String?,              // resolve to InventoryItem (skip if no tracked item — FR-004)
    val productVariantId: String?,
    val quantity: BigDecimal,            // positive
    val unitCost: BigDecimal? = null,
)
```

## Behavior

1. **Resolve item**: map each line's product/variant → `InventoryItem` at the default warehouse. If no
   inventory item exists, **skip that line** (inventory tracking is opt-in — FR-004); do not block the sale.
2. **Policy gate** (read via `SettingService.getBoolean("inventory", key)` — central setting module, R11):
   - If `inventory/auto_deduct_on_order = false` → no-op (manual flow only — FR-010/scenario 5).
   - If `inventory/block_orders_when_out_of_stock = true` AND a line would drive on-hand < 0 AND
     `inventory/allow_negative_stock = false` → throw `InsufficientStockException` (sale rejected, nothing
     applied — FR-013).
   - If `inventory/allow_negative_stock = true` → permit negative on-hand (FR-014).
3. **Idempotency** (R2): each line creates one `InventoryTransaction` with
   `(source_type, source_id, source_line_uid)`. The partial unique constraint makes a retry/duplicate a
   no-op (skip if exists). `applySale` and `reverseSale` are therefore safe under at-least-once delivery
   (FR-012/SC-002).
4. **Atomicity**: a single `applySale` is one DB transaction; either all eligible lines apply or none.
5. **Movements**: `applySale` → STOCK_OUT (reason SALE); `reverseSale` → STOCK_IN (reason RETURN), with
   `source_type = RETURN`, referencing the original document. Each computes `balance_after` (R7).

## Trigger wiring (R3 — proposed, confirm with order/invoice owners)

| Sale event | Call |
|---|---|
| Order confirmed (order-first workspaces) | `applySale(ORDER, orderUid, lines)` |
| Invoice finalized (invoice-first workspaces) | `applySale(INVOICE, invoiceUid, lines)` |
| Order cancelled | `reverseSale(ORDER, orderUid, affectedLines)` |
| Return / credit note | `reverseSale(RETURN, returnUid|invoiceUid, returnedLines)` |
| Invoice voided/cancelled | `reverseSale(INVOICE, invoiceUid, lines)` |

Preferred delivery: **explicit call** from order/invoice services to `InventoryStockService` (testable,
boundary-clean). Fallback: keep `InventoryOrderEventListener` consuming existing Spring events and calling
the same service. Idempotency makes a belt-and-suspenders combination safe.

**Open item**: confirm whether the canonical trigger is order-confirm, invoice-finalize, or both per
workspace mode, with the order/invoice owners (does not block inventory-side design).

## Test obligations (feeds quickstart.md & tasks.md)

- Idempotency: invoking `applySale` twice for the same command yields one movement and one deduction.
- Partial return: `reverseSale` with a subset of lines restores only those quantities.
- Policy matrix: {autoDeduct on/off} × {block on/off} × {negative on/off} behaves per the table above.
- Concurrency: concurrent `applySale` + manual adjustment on the same item leave consistent on-hand and
  monotonic `balance_after` ordering.
- Untracked product: a sale line with no inventory item is skipped without error.
