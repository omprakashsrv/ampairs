# Data Model: Database Schema Migration

**Feature**: Database Schema Migration with Flyway
**Date**: 2025-10-12
**Related**: [research.md](./research.md) | [plan.md](./plan.md) | [spec.md](./spec.md)

## Overview

This document provides entity-to-table mappings for all 22+ JPA entities requiring Flyway migration scripts. For detailed JPA annotation mapping rules and examples, see [research.md Decision 2](./research.md#decision-2-entity-to-ddl-conversion-pattern).

## Entity Dependency Order

1. `file` (core) provides binary metadata referenced by product imagery and other modules.
2. `unit` and `unit_conversion` (unit) define measurement references for product and inventory records.
3. Customer master data (`customer_groups`, `customer_types`, `master_states`, `state`, `customer`, `customer_images`) relies on unit/core only.
4. Product catalog tables (`product_group`, `product_brand`, `product_category`, `product_sub_category`, `product`, `product_image`, `product_price`) depend on core/unit/customer data.
5. Inventory tables (`inventory`, `inventory_transaction`, `inventory_unit_conversion`) reference product and unit data for stock tracking.
6. Sales order tables (`customer_order`, `order_item`) depend on customer, product, and inventory identifiers.
7. Invoice tables (`invoice`, `invoice_item`) depend on order and customer data.
8. Dynamic form tables (`attribute_definition`, `field_config`) attach to workspace and do not introduce additional dependencies but should run after tenant-aware tables exist.

---

## Base Domain Patterns

### BaseDomain (All Entities)

```sql
id BIGINT NOT NULL AUTO_INCREMENT,
uid VARCHAR(200) NOT NULL UNIQUE,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
last_updated BIGINT NOT NULL DEFAULT 0,
PRIMARY KEY (id),
INDEX idx_{table}_uid (uid)
```

### OwnableBaseDomain (Tenant-Scoped Entities)

Extends BaseDomain with:
```sql
owner_id VARCHAR(200) NOT NULL,  -- @TenantId for multi-tenancy
ref_id VARCHAR(255)               -- External system reference
```

---

## Migration V4_1: Core Module

### File Table
**Entity**: `com.ampairs.core.domain.model.File`
**Table**: `file`
**Extends**: BaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| storage_url | VARCHAR(500) | NOT NULL | S3/storage path |
| file_name | VARCHAR(255) | NOT NULL | Original filename |
| file_type | VARCHAR(50) | NOT NULL | MIME type |
| file_size | BIGINT | NOT NULL | Bytes |
| uploaded_by | VARCHAR(200) | NULL | User ID |

### Address Table (Optional - May be Embedded)
**Entity**: `com.ampairs.core.domain.model.Address`
**Note**: Check if persisted as standalone table or embedded type

---

## Migration V4_2: Unit Module

### Unit Table
**Entity**: `com.ampairs.unit.domain.model.Unit`
**Table**: `unit`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Default | Notes |
|--------|------|----------|---------|-------|
| name | VARCHAR(10) | NOT NULL | - | "KG", "Liter" |
| short_name | VARCHAR(10) | NOT NULL | - | "kg", "l" |
| decimal_places | INT | NOT NULL | 2 | Precision |
| active | BOOLEAN | NOT NULL | TRUE | Soft delete flag |

**Indexes**:
- `INDEX unit_idx (name)`
- `UNIQUE INDEX idx_unit_uid (uid)`

### UnitConversion Table
**Entity**: `com.ampairs.unit.domain.model.UnitConversion`
**Table**: `unit_conversion`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| base_unit_id | VARCHAR(200) | NOT NULL | FK  unit.uid |
| derived_unit_id | VARCHAR(200) | NOT NULL | FK  unit.uid |
| multiplier | DOUBLE | NOT NULL | Conversion factor |
| active | BOOLEAN | NOT NULL | TRUE |

**Foreign Keys**:
- `FOREIGN KEY (base_unit_id) REFERENCES unit(uid) ON DELETE CASCADE`
- `FOREIGN KEY (derived_unit_id) REFERENCES unit(uid) ON DELETE CASCADE`

**Indexes**:
- `INDEX idx_unit_conversion_base (base_unit_id)`
- `INDEX idx_unit_conversion_derived (derived_unit_id)`

---

## Migration V4_3: Customer Module

### CustomerGroup Table
**Entity**: `com.ampairs.customer.domain.model.CustomerGroup`
**Table**: `customer_group`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| name | VARCHAR(255) | NOT NULL | Group name |
| description | TEXT | NULL | Description |

### CustomerType Table
**Entity**: `com.ampairs.customer.domain.model.CustomerType`
**Table**: `customer_type`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| name | VARCHAR(255) | NOT NULL | "B2B", "B2C" |
| description | TEXT | NULL | Description |

### MasterState Table
**Entity**: `com.ampairs.customer.domain.model.MasterState`
**Table**: `master_state`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| name | VARCHAR(100) | NOT NULL | State name |
| state_code | VARCHAR(10) | NOT NULL | "MH", "KA" |
| country | VARCHAR(50) | NOT NULL | "India" |

### Customer Table
**Entity**: `com.ampairs.customer.domain.model.Customer`
**Table**: `customer`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| country_code | INT | NOT NULL | 91 for India |
| name | VARCHAR(255) | NOT NULL | Customer name |
| customer_type | VARCHAR(100) | NOT NULL | Type code |
| customer_group | VARCHAR(100) | NOT NULL | Group code |
| phone | VARCHAR(20) | NOT NULL | Contact |
| status | VARCHAR(20) | NOT NULL | "ACTIVE" |
| landline | VARCHAR(12) | NOT NULL | Landline |
| email | VARCHAR(255) | NOT NULL | Email |
| gst_number | VARCHAR(15) | NULL | GST number |
| pan_number | VARCHAR(10) | NULL | PAN number |
| credit_limit | DOUBLE | NOT NULL | 0.0 |
| credit_days | INT | NOT NULL | 0 |
| outstanding_amount | DOUBLE | NOT NULL | 0.0 |
| address | VARCHAR(255) | NOT NULL | Address line |
| street | VARCHAR(255) | NOT NULL | Street |
| street2 | VARCHAR(255) | NOT NULL | Street 2 |
| city | VARCHAR(255) | NOT NULL | City |
| pincode | VARCHAR(10) | NOT NULL | Pincode |
| state | VARCHAR(20) | NOT NULL | State |
| country | VARCHAR(20) | NOT NULL | "India" |
| location | POINT | NULL | Geolocation |
| billing_address | JSON | NOT NULL | Address object |
| shipping_address | JSON | NULL | Address object |

**Indexes**:
- `INDEX idx_customer_name (name)`
- `INDEX idx_customer_phone (phone)`
- `INDEX idx_customer_email (email)`
- `UNIQUE INDEX idx_customer_gst (gst_number)` (if not null)

### CustomerImage Table
**Entity**: `com.ampairs.customer.domain.model.CustomerImage`
**Table**: `customer_image`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| customer_uid | VARCHAR(200) | NOT NULL | FK  customer.uid |
| storage_url | VARCHAR(500) | NOT NULL | Image path |

**Foreign Keys**:
- `FOREIGN KEY (customer_uid) REFERENCES customer(uid) ON DELETE CASCADE`

---

## Migration V4_4: Product Module

### ProductGroup Table
**Entity**: `com.ampairs.product.domain.model.group.ProductGroup`
**Table**: `product_group`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| name | VARCHAR(255) | NOT NULL | Group name |

### ProductBrand Table
**Entity**: `com.ampairs.product.domain.model.group.ProductBrand`
**Table**: `product_brand`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| name | VARCHAR(255) | NOT NULL | Brand name |

### ProductCategory Table
**Entity**: `com.ampairs.product.domain.model.group.ProductCategory`
**Table**: `product_category`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| name | VARCHAR(255) | NOT NULL | Category name |

### ProductSubCategory Table
**Entity**: `com.ampairs.product.domain.model.group.ProductSubCategory`
**Table**: `product_sub_category`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| name | VARCHAR(255) | NOT NULL | Subcategory name |
| category_id | VARCHAR(200) | NULL | FK  product_category.uid |

### Product Table
**Entity**: `com.ampairs.product.domain.model.Product`
**Table**: `product`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| name | VARCHAR(255) | NOT NULL | Product name |
| code | VARCHAR(255) | NOT NULL | Product code |
| sku | VARCHAR(100) | NOT NULL | Unique SKU |
| description | TEXT | NULL | Description |
| status | VARCHAR(20) | NOT NULL | "ACTIVE" |
| tax_code_id | VARCHAR(36) | NULL | Tax code reference |
| tax_code | VARCHAR(20) | NOT NULL | Tax code |
| unit_id | VARCHAR(36) | NULL | FK  unit.uid |
| base_price | DOUBLE | NOT NULL | 0.0 |
| cost_price | DOUBLE | NOT NULL | 0.0 |
| group_id | VARCHAR(200) | NULL | FK  product_group.uid |
| brand_id | VARCHAR(200) | NULL | FK  product_brand.uid |
| category_id | VARCHAR(200) | NULL | FK  product_category.uid |
| sub_category_id | VARCHAR(200) | NULL | FK  product_sub_category.uid |
| base_unit_id | VARCHAR(200) | NULL | Base unit |
| mrp | DOUBLE | NOT NULL | 0.0 |
| dp | DOUBLE | NOT NULL | 0.0 |
| selling_price | DOUBLE | NOT NULL | 0.0 |
| index_no | INT | NOT NULL | 0 |
| attributes | JSON | NULL | Custom attributes |

**Indexes**:
- `UNIQUE INDEX idx_product_uid (uid)`
- `UNIQUE INDEX idx_product_sku (sku)`
- `INDEX idx_product_name (name)`

**Foreign Keys** (optional, based on implementation):
- `FOREIGN KEY (unit_id) REFERENCES unit(uid) ON DELETE SET NULL`

### ProductImage / ProductPrice / Inventory Tables
**Note**: See research.md Appendix B for complete table definitions

---

## Migration V4_5: Order Module

### Order Table
**Entity**: `com.ampairs.order.domain.model.Order`
**Table**: `customer_order`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| order_number | VARCHAR(255) | NOT NULL | Order ID |
| order_type | VARCHAR(20) | NOT NULL | "REGULAR" |
| customer_id | VARCHAR(36) | NULL | FK  customer.uid |
| customer_name | VARCHAR(255) | NULL | Customer name |
| customer_phone | VARCHAR(20) | NULL | Phone |
| is_walk_in | BOOLEAN | NOT NULL | Walk-in flag |
| payment_method | VARCHAR(20) | NOT NULL | "CASH" |
| invoice_ref_id | VARCHAR(255) | NULL | Invoice reference |
| order_date | TIMESTAMP | NOT NULL | Order date |
| delivery_date | TIMESTAMP | NULL | Delivery date |
| subtotal | DOUBLE | NOT NULL | 0.0 |
| discount_amount | DOUBLE | NOT NULL | 0.0 |
| tax_amount | DOUBLE | NOT NULL | 0.0 |
| total_amount | DOUBLE | NOT NULL | 0.0 |
| status | VARCHAR(20) | NOT NULL | "DRAFT" |
| notes | TEXT | NULL | Notes |

**Indexes**:
- `UNIQUE INDEX idx_order_uid (uid)`
- `UNIQUE INDEX order_ref_idx (ref_id)`
- `INDEX idx_order_customer (customer_id)`

### OrderItem Table
**Entity**: `com.ampairs.order.domain.model.OrderItem`
**Table**: `order_item`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| order_id | VARCHAR(200) | NOT NULL | FK  customer_order.uid |
| product_id | VARCHAR(200) | NOT NULL | FK  product.uid |
| quantity | DOUBLE | NOT NULL | Quantity |
| unit_price | DOUBLE | NOT NULL | Unit price |
| total_price | DOUBLE | NOT NULL | Total |

**Foreign Keys**:
- `FOREIGN KEY (order_id) REFERENCES customer_order(uid) ON DELETE CASCADE`
- `FOREIGN KEY (product_id) REFERENCES product(uid) ON DELETE RESTRICT`

---

## Migration V4_6: Invoice Module

### Invoice Table
**Entity**: `com.ampairs.invoice.domain.model.Invoice`
**Table**: `invoice`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| invoice_number | VARCHAR(255) | NOT NULL | Invoice ID |
| order_ref_id | VARCHAR(255) | NULL | FK  customer_order.ref_id |
| from_customer_id | VARCHAR(200) | NOT NULL | Seller |
| to_customer_id | VARCHAR(200) | NOT NULL | Buyer |
| invoice_date | TIMESTAMP | NOT NULL | Invoice date |
| due_date | TIMESTAMP | NULL | Due date |
| subtotal | DOUBLE | NOT NULL | 0.0 |
| tax_amount | DOUBLE | NOT NULL | 0.0 |
| total_amount | DOUBLE | NOT NULL | 0.0 |
| paid_amount | DOUBLE | NOT NULL | 0.0 |
| status | VARCHAR(20) | NOT NULL | "DRAFT" |
| notes | TEXT | NULL | Notes |

**Indexes**:
- `UNIQUE INDEX idx_invoice_number (invoice_number)`
- `INDEX idx_invoice_customer (to_customer_id)`

### InvoiceItem Table
**Entity**: `com.ampairs.invoice.domain.model.InvoiceItem`
**Table**: `invoice_item`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| invoice_id | VARCHAR(200) | NOT NULL | FK  invoice.uid |
| product_id | VARCHAR(200) | NOT NULL | FK  product.uid |
| quantity | DOUBLE | NOT NULL | Quantity |
| unit_price | DOUBLE | NOT NULL | Unit price |
| total_price | DOUBLE | NOT NULL | Total |

**Foreign Keys**:
- `FOREIGN KEY (invoice_id) REFERENCES invoice(uid) ON DELETE CASCADE`
- `FOREIGN KEY (product_id) REFERENCES product(uid) ON DELETE RESTRICT`

---

## Migration V4_7: Form Module (Optional)
### AttributeDefinition Table
**Entity**: `com.ampairs.form.domain.model.AttributeDefinition`
**Table**: `attribute_definition`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| attribute_name | VARCHAR(255) | NOT NULL | Attribute name |
| attribute_type | VARCHAR(50) | NOT NULL | "STRING", "NUMBER" |
| is_required | BOOLEAN | NOT NULL | Required flag |

### FieldConfig Table
**Entity**: `com.ampairs.form.domain.model.FieldConfig`
**Table**: `field_config`
**Extends**: OwnableBaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| field_name | VARCHAR(255) | NOT NULL | Field name |
| field_type | VARCHAR(50) | NOT NULL | Field type |
| validation_rules | JSON | NULL | Validation rules |

## Migration V4_8: Auth Module

### Device Session Table
**Entity**: `com.ampairs.auth.model.DeviceSession`
**Table**: `device_session`
**Extends**: BaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| user_id | VARCHAR(200) | NOT NULL | Workspace user identifier |
| device_id | VARCHAR(200) | NOT NULL | Device fingerprint |
| device_name | VARCHAR(255) | NULL | Human readable label |
| device_type | VARCHAR(50) | NULL | mobile/desktop/tablet |
| platform | VARCHAR(50) | NULL | OS family (iOS/Android/etc.) |
| browser | VARCHAR(100) | NULL | Browser name |
| os | VARCHAR(100) | NULL | OS version |
| ip_address | VARCHAR(45) | NULL | IPv4/IPv6 string |
| user_agent | VARCHAR(500) | NULL | Raw UA string |
| location | VARCHAR(255) | NULL | Optional geo lookup |
| last_activity | TIMESTAMP | NOT NULL | Defaults to CURRENT_TIMESTAMP |
| login_time | TIMESTAMP | NOT NULL | Session created at |
| is_active | BOOLEAN | NOT NULL | TRUE for active sessions |
| refresh_token_hash | VARCHAR(500) | NULL | Stored token hash |
| expired_at | TIMESTAMP | NULL | Soft expiration timestamp |

### Login Session Table
**Entity**: `com.ampairs.auth.model.LoginSession`
**Table**: `login_session`
**Extends**: BaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| country_code | INT | NOT NULL | Dialling code |
| phone | VARCHAR(12) | NOT NULL | Subscriber number |
| user_agent | VARCHAR(255) | NOT NULL | Client UA |
| expiry_time | TIMESTAMP | NOT NULL | OTP expiry |
| code | VARCHAR(255) | NULL | OTP value |
| attempts | INT | NOT NULL | Defaults to 0 |
| verified | BOOLEAN | NOT NULL | OTP verified flag |
| verified_at | TIMESTAMP | NULL | Verification timestamp |
| expired | BOOLEAN | NOT NULL | Hard expire flag |
| status | VARCHAR(50) | NOT NULL | `VerificationStatus` enum |

### Auth Token Table
**Entity**: `com.ampairs.auth.model.Token`
**Table**: `auth_token`
**Extends**: BaseDomain

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| token | VARCHAR(500) | NOT NULL | Signed JWT or refresh token |
| expired | BOOLEAN | NOT NULL | Soft expiry flag |
| revoked | BOOLEAN | NOT NULL | Revocation flag |
| user_id | VARCHAR(200) | NOT NULL | References `user.uid` |
| device_id | VARCHAR(200) | NULL | Optional device reference |
| token_type | VARCHAR(20) | NOT NULL | `TokenType` enum |



---

## Summary Statistics

**Total Entities**: 22
**Migrations Required**: 8 (V4_1 through V4_8)
**Total Tables**: 22+
**Foreign Key Relationships**: ~15
**Multi-Tenant Tables**: All (via OwnableBaseDomain)

**For Complete Details**:
- JPA annotation mapping: [research.md#decision-2](./research.md#decision-2-entity-to-ddl-conversion-pattern)
- Dependency ordering: [research.md#decision-3](./research.md#decision-3-dependency-ordering-and-table-creation-sequence)
- Full table definitions: [research.md#appendix-b](./research.md#appendix-b-entity-to-migration-mapping)
