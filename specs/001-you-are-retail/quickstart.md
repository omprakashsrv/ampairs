# Quickstart Guide: Unified Retail Management Platform

**Phase**: 1 - Design & Contracts  
**Date**: 2025-01-06  
**Status**: Complete  

## Overview

This quickstart guide provides step-by-step scenarios to validate the unified retail management platform functionality. Each scenario represents a user story from the specification and serves as integration test validation.

## Prerequisites

- Spring Boot application running on `http://localhost:8080`
- Valid JWT token for authentication
- PostgreSQL database initialized with multi-tenant support
- Sample workspace created with basic configuration

## Test Scenarios

### Scenario 1: Multi-tenant Workspace Setup

**User Story**: As a business owner, I need to create and configure my workspace with appropriate business type and modules.

**Steps**:
1. **Create Workspace**
   ```bash
   POST /workspace/v1
   {
     "name": "Hardware Store ABC",
     "slug": "hardware-abc",
     "business_type": "HARDWARE",
     "description": "Local hardware store",
     "enabled_modules": [
       "PRODUCT_MANAGEMENT",
       "INVENTORY", 
       "ORDER_MANAGEMENT",
       "INVOICE_GENERATION",
       "CUSTOMER_MANAGEMENT"
     ]
   }
   ```
   **Expected**: 201 Created, workspace created with unique ID

2. **Get Workspace Details**
   ```bash
   GET /workspace/v1/{workspaceId}
   ```
   **Expected**: 200 OK, all workspace details returned

3. **Invite Team Member**
   ```bash
   POST /workspace/v1/{workspaceId}/members
   {
     "email_or_phone": "manager@hardware-abc.com",
     "role": "MANAGER",
     "message": "Welcome to Hardware Store ABC team"
   }
   ```
   **Expected**: 201 Created, invitation sent

**Validation**: Workspace isolates all data, members have proper role-based access

---

### Scenario 2: Product Catalog Management

**User Story**: As a store manager, I need to add products with categories, pricing, and inventory tracking.

**Steps**:
1. **Create Tax Code** (prerequisite)
   ```bash
   POST /tax-code/v1
   {
     "name": "GST 18%",
     "tax_type": "GST",
     "rate": 18.00,
     "components": [
       {"name": "SGST", "rate": 9.00},
       {"name": "CGST", "rate": 9.00}
     ]
   }
   ```
   **Expected**: 201 Created, tax code with components

2. **Create Product**
   ```bash
   POST /product/v1
   {
     "name": "Steel Hammer 500g",
     "sku": "HAM-ST-500",
     "description": "Heavy duty steel hammer",
     "unit_id": "unit-pieces",
     "tax_code_id": "{taxCodeId}",
     "base_price": 450.00,
     "cost_price": 300.00
   }
   ```
   **Expected**: 201 Created, product with pricing

3. **Update Inventory**
   ```bash
   PUT /product/v1/{productId}/inventory
   {
     "adjustment_type": "SET",
     "quantity": 50,
     "reason": "Initial stock"
   }
   ```
   **Expected**: 200 OK, inventory set to 50 units

4. **Search Products**
   ```bash
   GET /product/v1/list?search=hammer&status=ACTIVE
   ```
   **Expected**: 200 OK, products matching search criteria

**Validation**: Products searchable, inventory tracked, tax calculations correct

---

### Scenario 3: Customer Management

**User Story**: As a sales person, I need to manage customer profiles with GST details and credit limits.

**Steps**:
1. **Create Business Customer**
   ```bash
   POST /customer/v1
   {
     "name": "ABC Construction Ltd",
     "email": "orders@abc-construction.com",
     "phone": "+91-9876543210",
     "gst_number": "07AAACR5055K1Z5",
     "customer_type": "BUSINESS",
     "address": {
       "line1": "123 Industrial Area",
       "city": "Mumbai",
       "state": "Maharashtra",
       "pincode": "400001"
     },
     "credit_limit": 50000.00,
     "payment_terms": "Net 30 days"
   }
   ```
   **Expected**: 201 Created, customer with GST validation

2. **Create Individual Customer**
   ```bash
   POST /customer/v1
   {
     "name": "Rajesh Kumar",
     "phone": "+91-9123456789",
     "customer_type": "INDIVIDUAL"
   }
   ```
   **Expected**: 201 Created, individual customer

3. **Search Customers**
   ```bash
   GET /customer/v1/list?search=construction&customer_type=BUSINESS
   ```
   **Expected**: 200 OK, business customers matching search

**Validation**: GST number validation, credit limits enforced, search functionality

---

### Scenario 4: Order Processing Workflow

**User Story**: As a sales person, I need to create orders, track inventory reservations, and manage order status.

**Steps**:
1. **Create Draft Order**
   ```bash
   POST /order/v1
   {
     "customer_id": "{customerId}",
     "line_items": [
       {
         "product_id": "{hammerId}",
         "quantity": 5,
         "unit_price": 450.00
       },
       {
         "product_id": "{screwdriverId}",
         "quantity": 10,
         "unit_price": 120.00
       }
     ],
     "discount_amount": 50.00,
     "notes": "Bulk order discount applied"
   }
   ```
   **Expected**: 201 Created, order in DRAFT status

2. **Confirm Order**
   ```bash
   PUT /order/v1/{orderId}/status
   {
     "status": "CONFIRMED",
     "notes": "Order confirmed by customer"
   }
   ```
   **Expected**: 200 OK, inventory reserved, status updated

3. **Check Inventory Impact**
   ```bash
   GET /product/v1/{hammerId}/inventory
   ```
   **Expected**: 200 OK, reserved_stock = 5, available_stock reduced

4. **Update Order Status to Fulfilled**
   ```bash
   PUT /order/v1/{orderId}/status
   {
     "status": "FULFILLED",
     "notes": "Items delivered"
   }
   ```
   **Expected**: 200 OK, inventory movement recorded

**Validation**: Stock reservation, status workflow enforcement, inventory synchronization

---

### Scenario 5: Invoice Generation and Payment

**User Story**: As an accountant, I need to convert orders to invoices, send them to customers, and track payments.

**Steps**:
1. **Convert Order to Invoice**
   ```bash
   POST /order/v1/{orderId}/convert-to-invoice
   {
     "due_date": "2025-02-06",
     "payment_terms": "Net 30 days",
     "invoice_notes": "Thank you for your business"
   }
   ```
   **Expected**: 201 Created, invoice with proper tax calculations

2. **Send Invoice to Customer**
   ```bash
   POST /invoice/v1/{invoiceId}/send
   {
     "send_via": ["EMAIL", "WHATSAPP"],
     "custom_message": "Your invoice is ready"
   }
   ```
   **Expected**: 200 OK, invoice status updated to SENT

3. **Record Partial Payment**
   ```bash
   POST /invoice/v1/{invoiceId}/payments
   {
     "amount": 1500.00,
     "payment_method": "UPI",
     "payment_date": "2025-01-10",
     "reference_number": "UPI123456789"
   }
   ```
   **Expected**: 201 Created, invoice status updated to PARTIAL_PAID

4. **Record Full Payment**
   ```bash
   POST /invoice/v1/{invoiceId}/payments
   {
     "amount": 1320.00,
     "payment_method": "BANK_TRANSFER",
     "payment_date": "2025-01-15",
     "reference_number": "NEFT789123456"
   }
   ```
   **Expected**: 201 Created, invoice status updated to PAID

5. **Download Invoice PDF**
   ```bash
   GET /invoice/v1/{invoiceId}/pdf
   ```
   **Expected**: 200 OK, PDF file download

**Validation**: Tax calculations, payment tracking, status updates, PDF generation

---

### Scenario 6: Multi-Platform Synchronization

**User Story**: As a mobile user, I need to access the same data across web and mobile with offline capability.

**Steps**:
1. **Web: Create Product**
   ```bash
   # Via Angular web app
   POST /product/v1 [create product via web interface]
   ```
   **Expected**: Product created successfully

2. **Mobile: Sync and View Product**
   ```bash
   # Via mobile app offline-first sync
   GET /product/v1/list [mobile app syncs with backend]
   ```
   **Expected**: Product appears in mobile app after sync

3. **Mobile: Create Order Offline**
   ```bash
   # Mobile app creates order locally, queues for sync
   [Create order in mobile app while offline]
   ```
   **Expected**: Order stored locally

4. **Mobile: Sync When Online**
   ```bash
   # Mobile app syncs with backend when connection restored
   POST /order/v1 [order uploaded from mobile queue]
   ```
   **Expected**: Order synchronized to backend, visible in web app

**Validation**: Real-time synchronization, offline capability, conflict resolution

---

### Scenario 7: Business Analytics and Reporting

**User Story**: As a business owner, I need to view sales analytics and inventory reports.

**Steps**:
1. **View Sales Dashboard**
   ```bash
   GET /analytics/v1/dashboard?period=LAST_30_DAYS
   ```
   **Expected**: 200 OK, sales metrics, top products, revenue trends

2. **Low Stock Report**
   ```bash
   GET /inventory/v1/reports/low-stock
   ```
   **Expected**: 200 OK, products below reorder level

3. **Customer Transaction History**
   ```bash
   GET /customer/v1/{customerId}/transactions?from_date=2025-01-01&to_date=2025-01-31
   ```
   **Expected**: 200 OK, customer order and payment history

**Validation**: Data aggregation, report generation, date filtering

---

## Performance Validation

### Response Time Requirements
- API endpoints: < 200ms average response time
- Page load: < 2s for web application
- Mobile app launch: < 3s cold start
- Real-time sync: < 5s for data synchronization

### Concurrent User Testing
- Support 1000+ concurrent users
- Database connection pooling efficiency
- JWT token validation performance

### Data Integrity Validation
- Multi-tenant data isolation
- Inventory accuracy across operations
- Tax calculation precision (decimal accuracy)
- Audit trail completeness

## Security Validation

### Authentication & Authorization
- JWT token expiration handling
- Role-based access control (ADMIN, MANAGER, STAFF)
- Multi-device session management
- API rate limiting

### Data Protection
- Sensitive data encryption
- GST number validation
- Input sanitization
- SQL injection prevention

## Success Criteria

✅ **All scenarios execute without errors**  
✅ **Performance requirements met**  
✅ **Multi-tenant isolation verified**  
✅ **Real-time synchronization working**  
✅ **Offline capabilities functional**  
✅ **Tax calculations accurate**  
✅ **Role-based access enforced**  
✅ **Data integrity maintained**

## Next Steps

After successful quickstart validation:
1. Run complete integration test suite
2. Execute performance benchmarks
3. Validate security penetration tests
4. Test mobile offline scenarios
5. Verify backup and recovery procedures

**Design Status**: ✅ COMPLETE - All test scenarios defined
**Next Step**: Execute `/tasks` command to generate implementation tasks