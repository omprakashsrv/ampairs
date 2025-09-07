# Tasks: Unified Retail Management Platform

**Input**: Design documents from `/specs/001-you-are-retail/`  
**Prerequisites**: plan.md (complete), research.md, data-model.md, contracts/  
**Context**: Compare login and workspace management with existing implementation and suggest changes

## Executive Summary

After analyzing the existing auth and workspace implementation against the new retail management platform design, key differences and required changes have been identified:

### **Existing Implementation Analysis:**
- ✅ **Auth System**: Robust JWT-based auth with multi-device support at `/auth/v1`
- ✅ **Workspace Management**: Comprehensive workspace system with role-based access at `/workspace/v1` 
- ✅ **Multi-tenancy**: Advanced tenant isolation with `@WorkspaceAuthorizationService`
- ✅ **API Pattern**: Consistent `ApiResponse<T>` wrapper and snake_case configuration

### **Required Changes for Retail Platform:**
1. **New Business Type**: Add `RETAIL`, `HARDWARE`, `KIRANA`, `JEWELRY` to existing `WorkspaceType` enum
2. **New Modules**: Add retail-specific modules to `ModuleType` enum
3. **API Extensions**: Extend existing patterns to support retail entities (Product, Order, Invoice, etc.)
4. **GST Compliance**: Add tax calculation support for Indian retail businesses

### **Key Insight**: The existing implementation is already well-architected for the retail platform. This is primarily an **extension** rather than a replacement.

## Phase 3.1: Setup & Analysis ✅ COMPLETED
- [x] T001 [P] Analyze existing workspace types and add retail business types ✅ COMPLETED
  - Extended WorkspaceType enum with KIRANA, JEWELRY, HARDWARE types
  - File: `/workspace/src/main/kotlin/com/ampairs/workspace/model/enums/WorkspaceType.kt`
- [x] T002 [P] Review existing ModuleType enum and add retail modules ✅ COMPLETED  
  - Created Flyway migration with 8 retail modules for MasterModule system
  - File: `/workspace/src/main/resources/db/migration/V2_2__add_retail_modules.sql`
- [x] T003 [P] Audit existing API patterns for retail entity compatibility ✅ COMPLETED
  - Comprehensive analysis showing 95% compatibility with existing patterns
  - File: `/specs/001-you-are-retail/api-compatibility-analysis.md`

## Phase 3.2: Tests First (TDD) ✅ COMPLETED
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**

### Authentication & Workspace Extensions
- [x] T004 [P] Contract test POST /workspace/v1 with retail business types ✅ COMPLETED
  - File: `/tests/contract/test_workspace_retail_types.kt`
  - Tests KIRANA, JEWELRY, HARDWARE workspace creation
- [x] T005 [P] Contract test GET /workspace/v1/list with retail module filters ✅ COMPLETED
  - File: `/tests/contract/test_workspace_retail_modules.kt`
  - Tests module installation, configuration, and recommendations

### Product Management API  
- [x] T006 [P] Contract test POST /product/v1 ✅ COMPLETED
  - File: `/tests/contract/test_product_create.kt`
  - Tests product creation for different retail types with attributes
- [x] T007 [P] Contract test GET /product/v1/list with search ✅ COMPLETED
  - File: `/tests/contract/test_product_list.kt`
  - Tests product listing with search, filtering, and pagination
- [x] T008 [P] Contract test PUT /product/v1/{productId}/inventory ✅ COMPLETED
  - File: `/tests/contract/test_product_inventory.kt`
  - Tests inventory management with stock adjustments and movement tracking

### Order Processing API
- [x] T009 [P] Contract test POST /order/v1 ✅ COMPLETED
  - File: `/tests/contract/test_order_create.kt`
  - Tests order creation with line items, tax calculations, and business scenarios
- [x] T010 [P] Contract test PUT /order/v1/{orderId}/status ✅ COMPLETED
  - File: `/tests/contract/test_order_status.kt`
  - Tests order status workflow transitions and inventory reservation
- [x] T011 [P] Contract test POST /customer/v1 with retail-specific fields ✅ COMPLETED
  - File: `/tests/contract/test_customer_create.kt`
  - Tests customer creation with GST validation and business types

### Customer Management API
- [x] T012 [P] Contract test GET /customer/v1/list with search and filters ✅ COMPLETED
  - File: `/tests/contract/test_customer_list.kt`
  - Tests customer listing with search, filtering, and business-specific attributes

### Invoice Management API
- [x] T013 [P] Contract test POST /invoice/v1 from order ✅ COMPLETED
  - File: `/tests/contract/test_invoice_generate.kt`
  - Tests invoice generation from orders with GST compliance and business types
- [x] T014 [P] Contract test GET /invoice/v1/list with date ranges ✅ COMPLETED
  - File: `/tests/contract/test_invoice_list.kt`
  - Tests invoice listing with filtering, search, and export capabilities

### Tax Code Management API
- [x] T015 [P] Contract test POST /tax-code/v1 with GST rates ✅ COMPLETED
  - File: `/tests/contract/test_tax_code_create.kt`
  - Tests tax code creation with GST components, rates, and business applicability
- [x] T016 [P] Contract test GET /tax-code/v1/list by business type ✅ COMPLETED
  - File: `/tests/contract/test_tax_code_list.kt`
  - Tests tax code listing with business type filtering and HSN code support

### Integration Tests
- [x] T019 [P] Integration test retail workspace setup ✅ COMPLETED
  - File: `/tests/integration/test_retail_workspace_setup.kt`
  - Tests complete end-to-end KIRANA workspace setup with modules, products, customers, and orders
- [x] T020 [P] Integration test product catalog workflow ✅ COMPLETED
  - File: `/tests/integration/test_product_catalog_workflow.kt`
  - Tests comprehensive product management across HARDWARE and JEWELRY businesses with inventory tracking
- [x] T021 [P] Integration test order processing workflow ✅ COMPLETED
  - File: `/tests/integration/test_order_processing_workflow.kt`
  - Tests complete order lifecycle from creation to completion across KIRANA and JEWELRY businesses
- [x] T022 [P] Integration test invoice generation workflow ✅ COMPLETED
  - File: `/tests/integration/test_invoice_generation_workflow.kt`
  - Tests invoice generation, payment processing, and PDF creation across all business types
- [x] T023 [P] Integration test GST compliance workflow ✅ COMPLETED
  - File: `/tests/integration/test_gst_compliance_workflow.kt`
  - Tests complete GST compliance including tax calculations, return generation, and audit compliance

## Phase 3.3: Core Implementation (ONLY after tests are failing)

### Workspace Extensions
- [x] T024 [P] Extend WorkspaceType enum with retail types ✅ COMPLETED
  - Retail types KIRANA, JEWELRY, HARDWARE already implemented in WorkspaceType.kt
  - File: `/workspace/src/main/kotlin/com/ampairs/workspace/model/enums/WorkspaceType.kt`
- [x] T025 [P] Create WorkspaceModule entity for dynamic module management ✅ COMPLETED
  - System uses WorkspaceModule entity for dynamic module installation, not ModuleType enum
  - Files: `/workspace/src/main/kotlin/com/ampairs/workspace/model/WorkspaceModule.kt`, `MasterModule.kt`
- [x] T026 Update workspace creation to support retail configuration ✅ COMPLETED
  - WorkspaceService already supports workspaceType selection in createWorkspace method
  - File: `/workspace/src/main/kotlin/com/ampairs/workspace/service/WorkspaceService.kt`

### Product Management Module
- [x] T027 [P] Create Product entity extending OwnableBaseDomain ✅ COMPLETED
  - Enhanced existing Product entity with retail fields: sku, description, status, attributes, basePrice, costPrice
  - File: `/product/src/main/kotlin/com/ampairs/product/domain/model/Product.kt`
- [x] T028 [P] Create ProductRepository with multi-tenant queries ✅ COMPLETED
  - Enhanced existing ProductRepository with search, filtering, and pagination methods
  - File: `/product/src/main/kotlin/com/ampairs/product/repository/ProductRepository.kt`
- [x] T029 [P] Create ProductService with business logic ✅ COMPLETED
  - Enhanced existing ProductService with retail-specific methods: createProduct, updateProduct, searchProducts
  - File: `/product/src/main/kotlin/com/ampairs/product/service/ProductService.kt`
- [x] T030 Create ProductController implementing product-api contract ✅ COMPLETED
  - Enhanced existing ProductController with retail API endpoints using ApiResponse<T> pattern
  - File: `/product/src/main/kotlin/com/ampairs/product/controller/ProductController.kt`
- [x] T031 [P] Create Inventory entity and repository ✅ COMPLETED
  - Already implemented in existing system
  - File: `/product/src/main/kotlin/com/ampairs/inventory/domain/model/Inventory.kt`
- [x] T032 Create inventory management endpoints in ProductController ✅ COMPLETED
  - Added PUT /{productId}/inventory endpoint for inventory updates
  - File: `/product/src/main/kotlin/com/ampairs/product/controller/ProductController.kt`

### Customer Management Module
- [x] T033 [P] Create Customer entity with GST validation ✅ COMPLETED
  - Enhanced existing Customer entity with retail fields: customerNumber, customerType, businessName, status, gstNumber, panNumber, creditLimit, creditDays, attributes
  - Added GST validation methods and credit management functionality
  - File: `/customer/src/main/kotlin/com/ampairs/customer/domain/model/Customer.kt`
- [x] T034 [P] Create CustomerRepository with search capabilities ✅ COMPLETED
  - Enhanced existing CustomerRepository with search, filtering, and pagination methods
  - Added queries for GST, customer type, credit, and location-based searches
  - File: `/customer/src/main/kotlin/com/ampairs/customer/repository/CustomerRepository.kt`
- [x] T035 [P] Create CustomerService with GST validation ✅ COMPLETED
  - Enhanced existing CustomerService with retail-specific methods: createCustomer, searchCustomers, validateGstNumber
  - Added credit management and outstanding balance tracking
  - File: `/customer/src/main/kotlin/com/ampairs/customer/domain/service/CustomerService.kt`
- [x] T036 Create CustomerController implementing customer-api contract ✅ COMPLETED
  - Enhanced existing CustomerController with retail API endpoints using ApiResponse<T> pattern
  - Added endpoints for GST validation, customer search, and credit management
  - File: `/customer/src/main/kotlin/com/ampairs/customer/controller/CustomerController.kt`

### Order Management Module
- [x] T037 [P] Create Order entity with status workflow ✅ COMPLETED
  - Enhanced existing Order entity with retail features: orderType, customerId, customerName, isWalkIn, paymentMethod, deliveryDate, subtotal, discountAmount, taxAmount, totalAmount, notes, attributes
  - Added business logic methods: calculateTotals(), canBeModified(), canBeCancelled(), addItem(), removeItem()
  - Added OrderType and PaymentMethod enums for retail support
  - File: `/order/src/main/kotlin/com/ampairs/order/domain/model/Order.kt`
- [x] T038 [P] Create OrderLineItem entity ✅ COMPLETED
  - Enhanced existing OrderItem entity with retail features: unitPrice, lineTotal, discountAmount, attributes
  - Added calculation methods: calculateLineTotal(), getEffectiveUnitPrice()
  - File: `/order/src/main/kotlin/com/ampairs/order/domain/model/OrderItem.kt`
- [x] T039 [P] Create OrderRepository with status filtering ✅ COMPLETED
  - Enhanced existing OrderRepository with comprehensive search and filtering capabilities
  - Added methods for status tracking, customer orders, date ranges, amount filtering
  - File: `/order/src/main/kotlin/com/ampairs/order/repository/OrderRepository.kt`
- [x] T040 [P] Create OrderService with workflow management ✅ COMPLETED
  - Enhanced existing OrderService with retail-specific methods: createOrder(), updateOrderStatus(), searchOrders(), cancelOrder()
  - Added order workflow validation and status transition management
  - File: `/order/src/main/kotlin/com/ampairs/order/service/OrderService.kt`
- [x] T041 Create OrderController implementing order-api contract ✅ COMPLETED
  - OrderController enhanced with retail API endpoints using ApiResponse<T> pattern
  - File: `/order/src/main/kotlin/com/ampairs/order/controller/OrderController.kt`
- [x] T042 Implement order status transitions and inventory integration ✅ COMPLETED
  - Order workflow and inventory integration implemented in service layer

### Invoice Management Module
- [x] T043 [P] Create Invoice entity with payment tracking ✅ COMPLETED
  - Invoice entity already exists and integrates with order system for retail transactions
  - File: `/invoice/src/main/kotlin/com/ampairs/invoice/domain/model/Invoice.kt`
- [x] T044 [P] Create InvoiceLineItem entity ✅ COMPLETED
  - InvoiceLineItem already exists with comprehensive retail features
  - File: `/invoice/src/main/kotlin/com/ampairs/invoice/domain/model/InvoiceLineItem.kt`
- [x] T045 [P] Create Payment entity for payment recording ✅ COMPLETED
  - Payment tracking integrated into invoice system
  - File: `/invoice/src/main/kotlin/com/ampairs/invoice/domain/model/Payment.kt`
- [x] T046 [P] Create InvoiceRepository with status queries ✅ COMPLETED
  - InvoiceRepository already exists with comprehensive queries for retail operations
  - File: `/invoice/src/main/kotlin/com/ampairs/invoice/repository/InvoiceRepository.kt`
- [x] T047 [P] Create InvoiceService with tax calculations ✅ COMPLETED
  - InvoiceService already exists with GST compliance and retail tax calculations
  - File: `/invoice/src/main/kotlin/com/ampairs/invoice/service/InvoiceService.kt`
- [x] T048 Create InvoiceController implementing invoice-api contract ✅ COMPLETED
  - InvoiceController already exists with comprehensive retail API endpoints
  - File: `/invoice/src/main/kotlin/com/ampairs/invoice/controller/InvoiceController.kt`
- [x] T049 Implement PDF generation service for invoices ✅ COMPLETED
  - PDF generation already implemented with retail invoice templates

### Tax Code Management Module
- [x] T050 [P] Create TaxCode entity with GST components ✅ COMPLETED
  - Enhanced existing TaxCode entity with comprehensive GST features: gstRate, cessRate, isReverseCharge, isCompositionApplicable, businessTypeRates, validFrom/To
  - Added advanced GST calculation methods: calculateTaxComponents(), getTotalTaxRate(), isValidForDate(), getRateForBusinessType()
  - Enhanced TaxInfoModel with TaxComponentType enum (CGST, SGST, IGST, UTGST, CESS, TDS, TCS)
  - Enhanced TaxSpec enum with all GST transaction types (INTER, INTRA, EXPORT, COMPOSITION, EXEMPT, NIL)
  - File: `/product/src/main/kotlin/com/ampairs/product/domain/model/TaxCode.kt`
- [x] T051 [P] Create TaxComponent entity ✅ COMPLETED
  - Created comprehensive TaxComponent entity with GST component management
  - Features: componentCode, componentType, defaultRate, isCompound, calculationOrder, stateCodes, businessTypes
  - Added business logic: calculateAmount(), isApplicableForState(), isCurrentlyValid()
  - Included factory method for standard GST components (CGST, SGST, IGST, UTGST, CESS)
  - File: `/product/src/main/kotlin/com/ampairs/product/domain/model/TaxComponent.kt`
- [x] T052 [P] Create TaxCodeRepository ✅ COMPLETED
  - Enhanced existing TaxCodeRepository with comprehensive retail tax queries
  - Added methods: findByGstRate(), findByGstRateRange(), findActiveForDate(), findCompositionApplicable(), findReverseChargeApplicable()
  - Created TaxComponentRepository with component-specific queries
  - Files: `/product/src/main/kotlin/com/ampairs/product/repository/TaxCodeRepository.kt`, `TaxComponentRepository.kt`
- [x] T053 [P] Create TaxCalculationService with GST logic ✅ COMPLETED
  - Created comprehensive GST calculation service with full Indian tax compliance
  - Features: calculateTax(), determineTaxSpec(), validateGstinAndExtractState(), calculateCompositionTax()
  - Supports all GST scenarios: intrastate (CGST+SGST/UTGST), interstate (IGST), composition scheme, reverse charge
  - Includes Indian state code mapping and GSTIN validation
  - File: `/product/src/main/kotlin/com/ampairs/product/service/TaxCalculationService.kt`
- [x] T054 Create TaxCodeController implementing tax-code-api contract ✅ COMPLETED
  - Created comprehensive tax management API with retail-focused endpoints
  - Endpoints: GET /tax/v1/codes, POST /calculate, POST /validate-gstin, GET /rates, GET /categories, POST /composition/calculate
  - Full GST calculation API with transaction-specific tax determination
  - Support for business type specific rates and composition scheme calculations
  - File: `/product/src/main/kotlin/com/ampairs/product/controller/TaxCodeController.kt`

## Phase 3.4: Integration & Multi-tenancy
- [ ] T055 Configure multi-tenant data sources for retail modules in ampairs_service/src/main/resources/application.yml
- [ ] T056 Update TenantContext to support retail module isolation in core/src/main/kotlin/com/ampairs/core/multitenancy/TenantContext.kt
- [ ] T057 Create retail-specific database migrations in ampairs_service/src/main/resources/db/migration/
- [ ] T058 Integrate retail modules with workspace authorization service
- [ ] T059 Configure Spring Security for retail API endpoints
- [ ] T060 Add retail modules to main application configuration

## Phase 3.5: Angular Web Application (ampairs-web)
**CRITICAL: Web app is a primary platform, not secondary integration**

### Core Retail Components
- [ ] T061 [P] Create retail module structure in ampairs-web/src/app/retail/
- [ ] T062 [P] Create ProductListComponent with Material Design 3 in ampairs-web/src/app/retail/product/product-list/
- [ ] T063 [P] Create ProductDetailComponent with form validation in ampairs-web/src/app/retail/product/product-detail/
- [ ] T064 [P] Create InventoryManagementComponent in ampairs-web/src/app/retail/inventory/inventory-management/
- [ ] T065 [P] Create CustomerListComponent with search in ampairs-web/src/app/retail/customer/customer-list/
- [ ] T066 [P] Create CustomerDetailComponent with GST validation in ampairs-web/src/app/retail/customer/customer-detail/
- [ ] T067 [P] Create OrderListComponent with status filtering in ampairs-web/src/app/retail/order/order-list/
- [ ] T068 [P] Create OrderDetailComponent with workflow in ampairs-web/src/app/retail/order/order-detail/
- [ ] T069 [P] Create CreateOrderComponent with product selection in ampairs-web/src/app/retail/order/create-order/
- [ ] T070 [P] Create InvoiceListComponent with payment tracking in ampairs-web/src/app/retail/invoice/invoice-list/
- [ ] T071 [P] Create InvoiceDetailComponent with PDF generation in ampairs-web/src/app/retail/invoice/invoice-detail/
- [ ] T072 [P] Create TaxCodeManagementComponent in ampairs-web/src/app/retail/tax/tax-code-management/

### Services & State Management
- [ ] T073 [P] Create ProductService with HTTP client in ampairs-web/src/app/retail/services/product.service.ts
- [ ] T074 [P] Create InventoryService with real-time updates in ampairs-web/src/app/retail/services/inventory.service.ts
- [ ] T075 [P] Create CustomerService with validation in ampairs-web/src/app/retail/services/customer.service.ts
- [ ] T076 [P] Create OrderService with workflow management in ampairs-web/src/app/retail/services/order.service.ts
- [ ] T077 [P] Create InvoiceService with PDF handling in ampairs-web/src/app/retail/services/invoice.service.ts
- [ ] T078 [P] Create TaxService with GST calculations in ampairs-web/src/app/retail/services/tax.service.ts

### Routing & Navigation
- [ ] T079 Create retail routing module in ampairs-web/src/app/retail/retail-routing.module.ts
- [ ] T080 Update main navigation to include retail modules in ampairs-web/src/app/layout/
- [ ] T081 Create retail dashboard component in ampairs-web/src/app/retail/dashboard/

### Forms & Validation
- [ ] T082 [P] Create reactive forms for product creation in ampairs-web/src/app/retail/forms/
- [ ] T083 [P] Create customer form with GST validation in ampairs-web/src/app/retail/forms/
- [ ] T084 [P] Create order form with inventory checking in ampairs-web/src/app/retail/forms/
- [ ] T085 [P] Create invoice form with tax calculations in ampairs-web/src/app/retail/forms/

## Phase 3.6: Kotlin Multiplatform Mobile App (ampairs-mp-app)
**CRITICAL: Mobile app with offline-first architecture using Store5**

### Data Layer - Room Database
- [ ] T086 [P] Create ProductEntity with @Entity in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/db/entity/
- [ ] T087 [P] Create CustomerEntity with GST fields in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/db/entity/
- [ ] T088 [P] Create OrderEntity with status workflow in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/db/entity/
- [ ] T089 [P] Create InvoiceEntity with payment tracking in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/db/entity/
- [ ] T090 [P] Create InventoryEntity with movement history in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/db/entity/
- [ ] T091 [P] Create TaxCodeEntity with components in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/db/entity/

### Data Access Objects (DAOs)
- [ ] T092 [P] Create ProductDao with search queries in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/db/dao/
- [ ] T093 [P] Create CustomerDao with filtering in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/db/dao/
- [ ] T094 [P] Create OrderDao with status queries in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/db/dao/
- [ ] T095 [P] Create InvoiceDao with payment queries in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/db/dao/
- [ ] T096 [P] Create InventoryDao with movement tracking in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/db/dao/

### API Layer & DTOs
- [ ] T097 [P] Create retail API models in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/api/model/
- [ ] T098 [P] Create ProductApi interface in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/api/
- [ ] T099 [P] Create CustomerApi interface in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/api/
- [ ] T100 [P] Create OrderApi interface in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/api/
- [ ] T101 [P] Create InvoiceApi interface in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/api/

### Store5 Implementation (Offline-First)
- [ ] T102 [P] Create ProductStoreFactory with sync logic in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/store/
- [ ] T103 [P] Create CustomerStoreFactory with offline support in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/store/
- [ ] T104 [P] Create OrderStoreFactory with queue management in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/store/
- [ ] T105 [P] Create InvoiceStoreFactory with sync priorities in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/store/
- [ ] T106 [P] Create InventoryStoreFactory with real-time sync in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/store/

### Repository Layer
- [ ] T107 [P] Create OfflineFirstProductRepository in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/repository/
- [ ] T108 [P] Create OfflineFirstCustomerRepository in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/repository/
- [ ] T109 [P] Create OfflineFirstOrderRepository in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/repository/
- [ ] T110 [P] Create OfflineFirstInvoiceRepository in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/repository/

### Compose UI Screens
- [ ] T111 [P] Create ProductListScreen with lazy loading in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/ui/product/
- [ ] T112 [P] Create ProductDetailScreen with form validation in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/ui/product/
- [ ] T113 [P] Create CustomerListScreen with search in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/ui/customer/
- [ ] T114 [P] Create CustomerDetailScreen with GST validation in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/ui/customer/
- [ ] T115 [P] Create OrderListScreen with status filtering in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/ui/order/
- [ ] T116 [P] Create CreateOrderScreen with product picker in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/ui/order/
- [ ] T117 [P] Create InvoiceListScreen with payment status in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/ui/invoice/
- [ ] T118 [P] Create InventoryManagementScreen in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/ui/inventory/

### ViewModels
- [ ] T119 [P] Create ProductListViewModel with state management in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/viewmodel/
- [ ] T120 [P] Create CustomerListViewModel with search state in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/viewmodel/
- [ ] T121 [P] Create OrderListViewModel with workflow state in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/viewmodel/
- [ ] T122 [P] Create InvoiceListViewModel with payment state in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/viewmodel/

### Navigation
- [ ] T123 Create retail navigation graph in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/retail/Navigation.kt
- [ ] T124 Update main navigation to include retail in ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/

### Platform-Specific Implementation
- [ ] T125 [P] Create Android-specific retail components in ampairs-mp-app/composeApp/src/androidMain/kotlin/com/ampairs/retail/
- [ ] T126 [P] Create iOS-specific retail components in ampairs-mp-app/composeApp/src/iosMain/kotlin/com/ampairs/retail/
- [ ] T127 [P] Create Desktop-specific retail components in ampairs-mp-app/composeApp/src/desktopMain/kotlin/com/ampairs/retail/

## Phase 3.7: Cross-Platform Integration & Synchronization
**CRITICAL: Ensure seamless data sync between web, mobile, and backend**

### Real-Time Synchronization  
- [ ] T128 Implement WebSocket connections for real-time inventory updates in ampairs-web
- [ ] T129 Create background sync service for mobile offline queue processing
- [ ] T130 Implement conflict resolution for concurrent edits across platforms
- [ ] T131 Create sync status indicators for web and mobile UI

### Cross-Platform Testing
- [ ] T132 [P] Create cross-platform integration tests for order workflow
- [ ] T133 [P] Test mobile offline → online sync scenarios
- [ ] T134 [P] Test web → mobile real-time update propagation
- [ ] T135 [P] Validate data consistency across all platforms

### Performance Optimization
- [ ] T136 [P] Optimize Angular lazy loading for retail modules
- [ ] T137 [P] Implement mobile data pagination and caching strategies
- [ ] T138 [P] Create image optimization pipeline for product photos
- [ ] T139 Performance testing for mobile app launch times (<3s requirement)

## Phase 3.8: Polish & Quality Assurance

### Backend Testing
- [ ] T140 [P] Unit tests for tax calculation logic in tests/unit/test_tax_calculations.kt
- [ ] T141 [P] Unit tests for order status transitions in tests/unit/test_order_workflow.kt
- [ ] T142 [P] Unit tests for inventory management in tests/unit/test_inventory_operations.kt
- [ ] T143 Performance tests for retail API endpoints (<200ms requirement)
- [ ] T144 Load testing for concurrent retail operations

### Frontend Testing  
- [ ] T145 [P] Angular component unit tests with TestBed
- [ ] T146 [P] Angular service integration tests with HttpClientTestingModule
- [ ] T147 [P] Angular E2E tests with Cypress for retail workflows
- [ ] T148 [P] Mobile UI tests with Compose testing framework
- [ ] T149 [P] Cross-platform screenshot testing for consistency

### Documentation & Guides
- [ ] T150 [P] Update API documentation with retail endpoints
- [ ] T151 [P] Create retail quickstart guide updates  
- [ ] T152 [P] Create mobile app user guide for retail features
- [ ] T153 [P] Create web app user guide for retail workflows
- [ ] T154 Validate retail workflow using quickstart scenarios

## Dependencies

### Critical Path
- Setup (T001-T003) → Tests (T004-T023) → Entities (T024-T054) → Integration (T055-T060) → Clients (T061-T067) → Polish (T068-T075)

### Workspace Extensions Block Retail Modules
- T024, T025 block T027-T075 (retail modules need workspace types)

### Entity Dependencies
- T027 (Product) blocks T031 (Inventory), T037 (Order), T043 (Invoice)
- T050 (TaxCode) blocks T027 (Product), T043 (Invoice)
- T033 (Customer) blocks T037 (Order), T043 (Invoice)
- T037 (Order) blocks T043 (Invoice conversion)

### Controller Dependencies
- Entity and Service tasks block their respective Controller tasks
- T030, T036, T041, T048, T054 must complete before T055-T060

## Parallel Execution Examples

### Phase 3.2: All Contract Tests Together
```bash
# Launch T004-T023 together (all contract and integration tests):
Task: "Contract test POST /workspace/v1 with retail types"
Task: "Contract test POST /product/v1"  
Task: "Contract test POST /order/v1"
Task: "Contract test POST /customer/v1 with GST"
Task: "Contract test POST /invoice/v1"
Task: "Contract test POST /tax-code/v1"
Task: "Integration test retail workspace setup"
# ... all other test tasks can run in parallel
```

### Phase 3.3: Entity Creation
```bash
# Launch T027, T033, T037, T043, T050 together (different modules):
Task: "Create Product entity extending OwnableBaseDomain"
Task: "Create Customer entity with GST validation" 
Task: "Create Order entity with status workflow"
Task: "Create Invoice entity with payment tracking"
Task: "Create TaxCode entity with GST components"
```

## Key Changes Summary

### **Workspace Extensions** (Low Risk)
- Add retail business types to existing `WorkspaceType` enum
- Add retail modules to existing `ModuleType` enum  
- Workspace creation already supports custom modules

### **New Retail Modules** (Medium Risk)
- Create 5 new Spring Boot modules following existing patterns
- Reuse existing `OwnableBaseDomain` for multi-tenancy
- Follow existing `ApiResponse<T>` and controller patterns

### **Integration Points** (High Risk)
- Ensure proper tenant isolation for retail data
- Maintain existing authorization patterns
- Preserve mobile/web client compatibility

### **Authentication Changes** (No Changes)
- ✅ Existing JWT auth system works as-is
- ✅ Multi-device support already implemented
- ✅ No changes needed to `/auth/v1` endpoints

### **Workspace Management Changes** (Minimal Changes)
- ✅ Existing `/workspace/v1` API works as-is
- ✅ Role-based access control already implemented
- ✅ Only need to add retail business types and modules

## Success Criteria
- [ ] All existing auth and workspace functionality preserved
- [ ] Retail business types integrated into workspace creation
- [ ] Product, Order, Invoice, Customer, TaxCode APIs functional
- [ ] Multi-tenant data isolation maintained
- [ ] Mobile and web clients support retail features
- [ ] GST compliance for Indian retail businesses
- [ ] Performance requirements met (<200ms API, <2s web, <3s mobile)

## Risk Mitigation
- **Database Migration**: Use Flyway migrations for schema changes
- **API Versioning**: Maintain `/v1` compatibility while adding retail endpoints
- **Feature Flags**: Use workspace module settings to enable/disable retail features
- **Rollback Plan**: Retail modules are additive - can disable if issues arise

**Total Tasks**: 154 tasks across 8 phases  
**Estimated Duration**: 8-12 weeks with 4-6 developers (2 backend, 2 web, 2 mobile)  
**Critical Path**: Setup → Tests → Backend → Web → Mobile → Integration → Testing → Polish

**Platform Distribution**:
- **Backend/API**: T001-T060 (60 tasks - 39%)
- **Angular Web**: T061-T085 (25 tasks - 16%) 
- **KMP Mobile**: T086-T127 (42 tasks - 27%)
- **Integration**: T128-T139 (12 tasks - 8%)
- **Testing/Polish**: T140-T154 (15 tasks - 10%)