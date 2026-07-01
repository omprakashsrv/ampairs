# Lazy Loading & DTO Mapping (avoid LazyInitializationException)

**Golden rule: never call a DTO mapper (`entity.asXResponse()`) in a controller on a JPA
entity returned from a service.** The transaction closes at the service boundary, so any lazy
collection touched by the mapper throws `LazyInitializationException: ... no session`.

Map entities → DTOs *inside* the service, while the transaction/session is still open. Controllers
receive DTOs and only wrap them in `ApiResponse` / `PageResponse`.

## Two-graph pattern (pick by query cardinality)

**Single-record queries** (`findByUid`, `findByEcomOrderRef`, …):
- Add a `@NamedEntityGraph` listing EVERY association the mapper touches (incl. `@OneToMany`).
- Apply `@EntityGraph("Entity.withAll")` on the repository method — JOIN FETCH initializes the
  collections in-memory before the session closes. Safe for one row.

**Paged queries** (`findBy...(pageable)`, `/sync` feeds):
- Do NOT add `@OneToMany` collections to a paged `@EntityGraph` — Hibernate falls back to
  in-memory pagination (`HHH90003004` warning) and loads the whole table.
- Instead: keep `@BatchSize(size = N)` on the collection field, and **map to DTO inside a
  `@Transactional(readOnly = true)` service method**. `@BatchSize` batch-loads all lazy
  collections for the page in a few queries instead of N+1.

```kotlin
// Entity — batch-load, do NOT put this collection in a paged graph
@OneToMany
@JoinColumn(name = "ecom_order_id", referencedColumnName = "uid", insertable = false, updatable = false)
@BatchSize(size = 50)
var lineItems: MutableList<EcomOrderLineItem> = mutableListOf()

// Service — map inside the read-only transaction (session still open)
@Transactional(readOnly = true)
fun getCustomerOrders(customerId: String, storefrontId: String, pageable: Pageable): Page<EcomOrderResponse> =
    orderRepository.findByCustomerIdAndStorefrontId(customerId, storefrontId, pageable)
        .map { it.asEcomOrderResponse() }

// Controller — no mapper call, just wrap
val page = orderService.getCustomerOrders(customerId, storefront.uid, pageable)
return ApiResponse.success(PageResponse.from(page))
```

## Checklist before returning a JPA entity from a service to a controller
- Does the DTO mapper read any `@OneToMany` / `@ManyToMany` / lazy `@ManyToOne`? If yes, either
  map in the service or ensure a single-record `@EntityGraph` initialized it.
- Paged endpoint? Map in the service; never attach collections to the paged graph.
- Regression hit in `ecom` (`EcomOrder.lineItems`) and `product` — treat every new paged +
  collection-bearing endpoint as suspect.