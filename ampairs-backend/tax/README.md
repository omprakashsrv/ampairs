# Tax Module

## Overview
The Tax module centralises GST (Goods and Services Tax) intelligence for Ampairs. It maintains catalogues of HSN/SAC codes, business type classifications, tax rate schedules, and configuration rules. The module exposes administrative APIs for maintaining tax metadata and provides the `GstTaxCalculationService`, which the order and invoice flows use to compute accurate tax components for each transaction.

## Architecture
### Package Structure
```
com.ampairs.tax/
├── config/                 # Module configuration (constants, bean setup)
├── controller/             # REST endpoints for tax rates, HSN codes, configurations, and calculations
├── domain/                 # Domain DTOs, enums, and JPA entities
│   ├── dto/                # Request/response payloads and calculation results
│   ├── enums/              # Tax component types, business types, zones, transaction types
│   └── model/              # Persistent entities for tax configuration, rates, HSN codes
├── repository/             # Spring Data repositories for tax metadata
└── service/                # Business services (GST calculation, rate management)
```

## Key Components
- **`TaxCalculationController`** – Public API for calculating GST on single or bulk transactions and previewing effective tax components.
- **`TaxConfigurationController`** – Administrative endpoints to manage configuration templates (reverse charge, exemptions, composition rates, etc.).
- **`TaxRateController`** and **`HsnCodeController`** – CRUD APIs for maintaining tax rate slabs and HSN master data.
- **`GstTaxCalculationService`** – Core calculation engine that determines transaction/geo context, applies configuration rules, aggregates tax components, and returns detailed breakdowns.
- **Repositories** – Provide effective-date queries (`findEffectiveConfigurationBy…`, `findByHsnCodeAndValidForDate`) to support historical rate management and future-dated updates.
- **Domain DTOs (`TaxCalculationDto`, `TaxRateDto`, `TaxConfigurationDto`)** – Expose calculation inputs/outputs with precise monetary fields and metadata for UI display.

## Features
- **Accurate GST math**: Calculates CGST, SGST, IGST, and cess components based on transaction type (intra/inter-state), business type, and effective dates.
- **HSN/SAC catalogue**: Stores HSN metadata with validity ranges, enabling date-aware lookups even after rate changes.
- **Configurable rules**: Supports reverse charge, exemptions, composition schemes, and geo-specific overrides through `TaxConfiguration`.
- **Bulk calculation**: Accepts multiple line items and returns aggregated totals alongside per-item tax breakdowns.
- **Validation safeguards**: Guards against invalid amounts, missing HSN codes, or inactive configurations and surfaces actionable error messages.
- **Audit-ready output**: Provides calculation notes, applied configuration identifiers, and breakdowns suitable for invoices and filings.

## API Highlights
- `POST /api/v1/tax/calculate` – Calculate GST for a single line item.
- `POST /api/v1/tax/calculate/bulk` – Calculate GST for a list of items and aggregate totals.
- `GET /api/v1/tax/hsn` / `POST /api/v1/tax/hsn` – Manage HSN code metadata.
- `GET /api/v1/tax/rates` / `POST /api/v1/tax/rates` – Manage tax rate slabs.
- `GET /api/v1/tax/configurations` / `POST /api/v1/tax/configurations` – Maintain tax configuration templates.

All endpoints adhere to the shared `ApiResponse<T>` response envelope and expect workspace tenancy headers for scoping.

## Integration Points
- Depends on `core` for base domain constructs, validation utilities, and API responses.
- Consumed by the `order` and `invoice` modules to compute tax amounts during checkout and document generation.
- Relies on `business` metadata for certain calculations (e.g., business type) and cooperates with `workspace` to ensure tenant scoping.
- Emits calculation results that front-end clients display in invoices, quotes, and analytics dashboards.

## Build & Test
```bash
# From ampairs-backend/
./gradlew :tax:build
./gradlew :tax:test
```

End-to-end validation is available through `./gradlew :ampairs_service:bootRun`, which exposes the tax APIs alongside dependent modules.
