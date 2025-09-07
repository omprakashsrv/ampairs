# Data Model: Unified Retail Management Platform

**Phase**: 1 - Design & Contracts  
**Date**: 2025-01-06  
**Status**: Complete  

## Overview

This document defines the core data entities and their relationships for the unified retail management platform, extracted from the feature specification and aligned with the existing codebase architecture.

## Core Entities

### 1. Workspace (Multi-tenant Business Environment)

**Purpose**: Multi-tenant business environment with configurable modules and business type settings

**Fields**:
```kotlin
data class Workspace {
    val id: String                    // Primary key
    val slug: String                  // URL-friendly identifier
    val name: String                  // Business name
    val businessType: BusinessType    // RETAIL, WHOLESALE, MANUFACTURING, SERVICE
    val description: String?          // Optional description
    val settings: WorkspaceSettings   // Configuration object
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
    val active: Boolean = true
    val softDeleted: Boolean = false
}

enum class BusinessType {
    RETAIL, WHOLESALE, MANUFACTURING, SERVICE, KIRANA, JEWELRY, HARDWARE
}

data class WorkspaceSettings {
    val enabledModules: List<ModuleType>
    val currency: String = "INR"
    val timezone: String = "Asia/Kolkata"
    val language: String = "en"
    val taxConfiguration: TaxConfiguration
}

enum class ModuleType {
    PRODUCT_MANAGEMENT, INVENTORY, ORDER_MANAGEMENT, 
    INVOICE_GENERATION, CUSTOMER_MANAGEMENT, ANALYTICS, 
    TALLY_INTEGRATION, NOTIFICATIONS
}
```

**Relationships**:
- One-to-many with Users (workspace membership)
- One-to-many with Products, Orders, Invoices, Customers
- Tenant isolation via workspace context

**Validation Rules**:
- Slug must be unique globally
- Name required, 3-100 characters
- At least one module must be enabled

---

### 2. User (Authenticated Individuals)

**Purpose**: Authenticated individuals with JWT sessions, device tracking, and workspace memberships

**Fields**:
```kotlin
data class User {
    val id: String                    // Primary key
    val phone: String                 // Phone number (unique)
    val email: String?                // Optional email
    val name: String                  // Full name
    val profileCompleted: Boolean = false
    val sessions: List<UserSession>   // Device sessions
    val workspaceMemberships: List<WorkspaceMembership>
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
    val active: Boolean = true
}

data class UserSession {
    val id: String
    val deviceId: String
    val deviceName: String?
    val deviceType: DeviceType        // WEB, ANDROID, IOS, DESKTOP
    val tokenHash: String
    val expiresAt: LocalDateTime
    val lastActiveAt: LocalDateTime
    val createdAt: LocalDateTime
}

data class WorkspaceMembership {
    val workspaceId: String
    val userId: String
    val role: WorkspaceRole          // ADMIN, MANAGER, STAFF
    val permissions: List<Permission>
    val joinedAt: LocalDateTime
    val active: Boolean = true
}

enum class DeviceType { WEB, ANDROID, IOS, DESKTOP }
enum class WorkspaceRole { ADMIN, MANAGER, STAFF }
```

**Relationships**:
- Many-to-many with Workspaces (through WorkspaceMembership)
- One-to-many with UserSessions

**Validation Rules**:
- Phone number must be valid format
- Each user-workspace combination must be unique
- At least one active session required for active users

---

### 3. Product (Catalog Items)

**Purpose**: Catalog items with categories, groups, brands, units, tax codes, and pricing information

**Fields**:
```kotlin
data class Product {
    val id: String                    // Primary key
    val workspaceId: String          // Tenant isolation
    val sku: String                   // Stock keeping unit
    val name: String                  // Product name
    val description: String?          // Optional description
    val categoryId: String?           // Product category
    val groupId: String?              // Product group
    val brandId: String?              // Product brand
    val unitId: String                // Unit of measurement
    val taxCodeId: String             // Tax configuration
    val basePrice: BigDecimal         // Base selling price
    val costPrice: BigDecimal?        // Optional cost price
    val images: List<ProductImage>    // Product images
    val attributes: Map<String, Any>  // Flexible attributes (weight for jewelry, etc.)
    val barcode: String?              // Optional barcode
    val status: ProductStatus         // ACTIVE, INACTIVE, DISCONTINUED
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
    val ownerId: String              // Tenant context
}

data class ProductImage {
    val id: String
    val url: String
    val alt: String?
    val isPrimary: Boolean = false
    val sortOrder: Int = 0
}

enum class ProductStatus { ACTIVE, INACTIVE, DISCONTINUED }
```

**Relationships**:
- Belongs to Workspace (tenant isolation)
- Many-to-one with Category, Group, Brand, Unit, TaxCode
- One-to-many with Inventory records
- Many-to-many with Orders (through OrderLineItem)

**Validation Rules**:
- SKU must be unique within workspace
- Name required, 1-200 characters
- Base price must be positive
- At least one active product required per workspace

---

### 4. Inventory (Stock Levels and Movements)

**Purpose**: Stock levels, movements, and tracking tied to products with real-time synchronization

**Fields**:
```kotlin
data class Inventory {
    val id: String                    // Primary key
    val workspaceId: String          // Tenant isolation
    val productId: String            // Product reference
    val locationId: String?          // Optional location/warehouse
    val currentStock: BigDecimal     // Current quantity
    val reservedStock: BigDecimal = BigDecimal.ZERO    // Reserved for orders
    val availableStock: BigDecimal   // Computed: current - reserved
    val reorderLevel: BigDecimal?    // Low stock threshold
    val maxStockLevel: BigDecimal?   // Maximum stock threshold
    val lastUpdated: LocalDateTime
    val ownerId: String              // Tenant context
}

data class InventoryMovement {
    val id: String
    val workspaceId: String
    val productId: String
    val movementType: MovementType   // IN, OUT, ADJUSTMENT, TRANSFER
    val quantity: BigDecimal
    val previousStock: BigDecimal
    val newStock: BigDecimal
    val reason: String?              // Optional reason
    val referenceId: String?         // Order/Invoice reference
    val referenceType: ReferenceType? // ORDER, INVOICE, ADJUSTMENT
    val timestamp: LocalDateTime
    val userId: String               // Who made the change
    val ownerId: String
}

enum class MovementType { IN, OUT, ADJUSTMENT, TRANSFER }
enum class ReferenceType { ORDER, INVOICE, ADJUSTMENT, PURCHASE, RETURN }
```

**Relationships**:
- Belongs to Workspace and Product
- One-to-many with InventoryMovements
- Referenced by OrderLineItems for stock checking

**Validation Rules**:
- Current stock cannot be negative
- Reserved stock cannot exceed current stock
- Movement quantity must be non-zero

---

### 5. Order (Sales Transactions)

**Purpose**: Sales transactions from creation to fulfillment with line items, quantities, and pricing

**Fields**:
```kotlin
data class Order {
    val id: String                    // Primary key
    val workspaceId: String          // Tenant isolation
    val orderNumber: String          // Human-readable order number
    val customerId: String?          // Optional customer
    val status: OrderStatus          // DRAFT, CONFIRMED, PROCESSING, FULFILLED, CANCELLED
    val orderDate: LocalDateTime
    val lineItems: List<OrderLineItem>
    val subtotal: BigDecimal         // Sum of line items
    val taxAmount: BigDecimal        // Total tax
    val discountAmount: BigDecimal = BigDecimal.ZERO
    val totalAmount: BigDecimal      // Subtotal + tax - discount
    val notes: String?               // Optional notes
    val createdBy: String            // User who created
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
    val ownerId: String              // Tenant context
}

data class OrderLineItem {
    val id: String
    val productId: String
    val quantity: BigDecimal
    val unitPrice: BigDecimal        // Price at time of order
    val taxRate: BigDecimal          // Tax rate applied
    val lineTotal: BigDecimal        // quantity * unitPrice
    val taxAmount: BigDecimal        // Tax for this line
    val discountAmount: BigDecimal = BigDecimal.ZERO
}

enum class OrderStatus { 
    DRAFT, CONFIRMED, PROCESSING, FULFILLED, CANCELLED, RETURNED 
}
```

**Relationships**:
- Belongs to Workspace
- Many-to-one with Customer (optional)
- One-to-many with OrderLineItems
- One-to-one with Invoice (when converted)
- References Products through OrderLineItems

**Validation Rules**:
- Order number must be unique within workspace
- At least one line item required
- Quantities must be positive
- Total amount must equal calculated amount

---

### 6. Invoice (Generated Billing Documents)

**Purpose**: Generated billing documents with tax calculations, payment status, and audit trails

**Fields**:
```kotlin
data class Invoice {
    val id: String                    // Primary key
    val workspaceId: String          // Tenant isolation
    val invoiceNumber: String        // Human-readable invoice number
    val orderId: String?             // Source order reference
    val customerId: String?          // Optional customer
    val invoiceDate: LocalDateTime
    val dueDate: LocalDateTime?      // Payment due date
    val lineItems: List<InvoiceLineItem>
    val subtotal: BigDecimal
    val taxAmount: BigDecimal
    val discountAmount: BigDecimal = BigDecimal.ZERO
    val totalAmount: BigDecimal
    val paidAmount: BigDecimal = BigDecimal.ZERO
    val balanceAmount: BigDecimal    // totalAmount - paidAmount
    val status: InvoiceStatus        // DRAFT, SENT, PAID, OVERDUE, CANCELLED
    val paymentTerms: String?        // Payment terms
    val notes: String?
    val gstDetails: GstDetails?      // GST compliance data
    val createdBy: String
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
    val ownerId: String
}

data class InvoiceLineItem {
    val id: String
    val productId: String
    val description: String          // Product name at time of invoice
    val quantity: BigDecimal
    val unitPrice: BigDecimal
    val taxRate: BigDecimal
    val lineTotal: BigDecimal
    val taxAmount: BigDecimal
    val discountAmount: BigDecimal = BigDecimal.ZERO
}

data class GstDetails {
    val gstNumber: String?
    val placeOfSupply: String
    val taxBreakup: Map<String, BigDecimal>  // SGST, CGST, IGST amounts
}

enum class InvoiceStatus { 
    DRAFT, SENT, PAID, PARTIAL_PAID, OVERDUE, CANCELLED 
}
```

**Relationships**:
- Belongs to Workspace
- Many-to-one with Customer (optional)
- Many-to-one with Order (source)
- One-to-many with Payments
- One-to-many with InvoiceLineItems

**Validation Rules**:
- Invoice number must be unique within workspace
- Due date must be after invoice date
- Balance amount must equal total minus paid
- GST details required for Indian businesses

---

### 7. Customer (Business Contacts)

**Purpose**: Business contacts with profiles, transaction history, and relationship management

**Fields**:
```kotlin
data class Customer {
    val id: String                    // Primary key
    val workspaceId: String          // Tenant isolation
    val name: String                  // Customer name
    val email: String?                // Optional email
    val phone: String?                // Optional phone
    val gstNumber: String?            // GST registration number
    val address: Address?             // Business address
    val customerType: CustomerType    // INDIVIDUAL, BUSINESS
    val creditLimit: BigDecimal?      // Optional credit limit
    val paymentTerms: String?         // Default payment terms
    val notes: String?                // Additional notes
    val status: CustomerStatus        // ACTIVE, INACTIVE
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
    val ownerId: String               // Tenant context
}

data class Address {
    val line1: String
    val line2: String?
    val city: String
    val state: String
    val country: String = "India"
    val pincode: String
}

enum class CustomerType { INDIVIDUAL, BUSINESS }
enum class CustomerStatus { ACTIVE, INACTIVE }
```

**Relationships**:
- Belongs to Workspace
- One-to-many with Orders
- One-to-many with Invoices
- One-to-many with CustomerTransactions (for history)

**Validation Rules**:
- Name required, 1-200 characters
- GST number must be valid format if provided
- Credit limit must be non-negative

---

### 8. TaxCode (Configurable Tax Rates)

**Purpose**: Configurable tax rates and structures for GST compliance and regional requirements

**Fields**:
```kotlin
data class TaxCode {
    val id: String                    // Primary key
    val workspaceId: String          // Tenant isolation
    val name: String                  // Tax code name (e.g., "GST 18%")
    val description: String?          // Optional description
    val taxType: TaxType             // GST, VAT, SERVICE_TAX
    val rate: BigDecimal             // Tax rate percentage
    val components: List<TaxComponent> // SGST, CGST, IGST breakdown
    val applicableFrom: LocalDate
    val applicableTo: LocalDate?     // Optional end date
    val status: TaxStatus            // ACTIVE, INACTIVE
    val createdAt: LocalDateTime
    val ownerId: String
}

data class TaxComponent {
    val name: String                 // SGST, CGST, IGST, VAT
    val rate: BigDecimal            // Component rate
    val accountCode: String?        // Accounting code
}

enum class TaxType { GST, VAT, SERVICE_TAX, EXCISE, CUSTOM }
enum class TaxStatus { ACTIVE, INACTIVE }
```

**Relationships**:
- Belongs to Workspace
- Referenced by Products
- Used in Order and Invoice calculations

**Validation Rules**:
- Tax rate must be between 0 and 100
- Component rates must sum to total rate
- Name must be unique within workspace

---

### 9. Notification (System-generated Alerts)

**Purpose**: System-generated alerts and messages for business events and user communications

**Fields**:
```kotlin
data class Notification {
    val id: String                    // Primary key
    val workspaceId: String          // Tenant isolation
    val userId: String               // Target user
    val type: NotificationType       // LOW_STOCK, PAYMENT_DUE, ORDER_STATUS, etc.
    val title: String                // Notification title
    val message: String              // Notification content
    val data: Map<String, Any>?      // Additional structured data
    val channels: List<NotificationChannel> // EMAIL, SMS, PUSH, IN_APP
    val priority: Priority           // LOW, MEDIUM, HIGH, URGENT
    val status: NotificationStatus   // PENDING, SENT, READ, FAILED
    val scheduledAt: LocalDateTime?  // When to send (for scheduled notifications)
    val sentAt: LocalDateTime?       // When actually sent
    val readAt: LocalDateTime?       // When user read it
    val expiresAt: LocalDateTime?    // Optional expiry
    val createdAt: LocalDateTime
    val ownerId: String
}

enum class NotificationType {
    LOW_STOCK, PAYMENT_DUE, ORDER_STATUS, INVOICE_SENT,
    WORKSPACE_INVITATION, SYSTEM_MAINTENANCE, BACKUP_COMPLETED
}

enum class NotificationChannel { EMAIL, SMS, PUSH, IN_APP }
enum class Priority { LOW, MEDIUM, HIGH, URGENT }
enum class NotificationStatus { PENDING, SENT, READ, FAILED }
```

**Relationships**:
- Belongs to Workspace and User
- May reference other entities via data field

**Validation Rules**:
- Title and message required
- Scheduled time must be in future
- At least one channel required

## Entity Relationships Summary

```
Workspace (1) ←→ (∞) User [WorkspaceMembership]
Workspace (1) ←→ (∞) Product
Workspace (1) ←→ (∞) Customer  
Workspace (1) ←→ (∞) Order
Workspace (1) ←→ (∞) Invoice
Workspace (1) ←→ (∞) TaxCode
Workspace (1) ←→ (∞) Notification

Product (1) ←→ (∞) Inventory
Product (1) ←→ (∞) OrderLineItem
Product (∞) ←→ (1) TaxCode

Order (1) ←→ (∞) OrderLineItem  
Order (1) ←→ (1) Invoice
Order (∞) ←→ (1) Customer

Invoice (1) ←→ (∞) InvoiceLineItem
Invoice (∞) ←→ (1) Customer

User (1) ←→ (∞) UserSession
User (1) ←→ (∞) Notification
```

## State Transitions

### Order Status Flow
```
DRAFT → CONFIRMED → PROCESSING → FULFILLED
   ↓         ↓           ↓
CANCELLED ← CANCELLED ← CANCELLED
                     ↓
                  RETURNED
```

### Invoice Status Flow
```
DRAFT → SENT → PARTIAL_PAID → PAID
   ↓      ↓         ↓
CANCELLED  ↓    OVERDUE
        OVERDUE
```

### Product Status Flow
```
ACTIVE ←→ INACTIVE → DISCONTINUED
```

## Multi-tenancy Implementation

All entities include `workspaceId` and `ownerId` fields for complete tenant isolation:
- **workspaceId**: Direct workspace association
- **ownerId**: Tenant context for Hibernate multi-tenancy

## Data Validation Summary

- All entities include audit fields (createdAt, updatedAt)
- Soft deletion supported via active/softDeleted flags  
- Multi-tenant isolation enforced at entity level
- Foreign key constraints maintain referential integrity
- Business rules enforced via validation annotations
- Unique constraints prevent duplicate data within tenants

**Design Status**: ✅ COMPLETE - All core entities defined with relationships and validation rules
**Next Step**: Generate API contracts from these entities