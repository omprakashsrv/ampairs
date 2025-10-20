# Feature Specification: Separate Unit Module

**Feature Branch**: `004-create-separate-unit`
**Created**: 2025-10-12
**Status**: Draft
**Input**: User description: "create separate unit module, de-structure the existing unit code from product, inventory and invoice modules. Just like as we did for tax module."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Access Centralized Units from Product Management (Priority: P1)

When business users manage products, they need to select measurement units (e.g., kg, meter, piece) from a shared catalog that is consistent across all features in the system.

**Why this priority**: Products depend on units for pricing, inventory tracking, and order processing. Without a centralized unit module, users face inconsistent unit definitions across different modules, leading to data integrity issues and conversion errors.

**Independent Test**: Create a product and assign it a measurement unit from the centralized unit catalog. Verify the unit appears correctly in product details and can be used for inventory and invoice calculations.

**Acceptance Scenarios**:

1. **Given** the unit module is deployed, **When** a user creates a product, **Then** they can select from all available units in the centralized unit catalog
2. **Given** a product has an assigned unit, **When** the user views product details, **Then** the unit information displays correctly with name and symbol
3. **Given** multiple products use the same unit, **When** the unit definition is updated in one place, **Then** all products reflect the updated unit information

---

### User Story 2 - Define Unit Conversions for Multi-Unit Products (Priority: P1)

Business users need to sell products in different units (e.g., sell rice in both kg and bags where 1 bag = 25 kg) and have the system automatically convert quantities for inventory and invoicing.

**Why this priority**: Many businesses need flexible unit handling for retail (sell by piece) vs wholesale (sell by case) scenarios. This is critical for accurate inventory management and pricing calculations.

**Independent Test**: Define a unit conversion (e.g., 1 box = 12 pieces), create a product, and verify that orders can be placed in either unit with automatic quantity conversion.

**Acceptance Scenarios**:

1. **Given** a product with defined unit conversions, **When** a user places an order in the derived unit (e.g., boxes), **Then** the system converts to the base unit (pieces) for inventory deduction
2. **Given** a unit conversion is defined, **When** a user views conversion details, **Then** both the multiplier and relationship between base and derived units are clearly displayed
3. **Given** a product has multiple unit conversions, **When** generating an invoice, **Then** the system uses the appropriate conversion based on the order unit

---

### User Story 3 - Manage Global Unit Catalog (Priority: P2)

System administrators need to create, update, and manage a centralized catalog of measurement units that can be used across all business modules (products, inventory, invoices).

**Why this priority**: A well-managed unit catalog ensures data consistency and prevents duplicate or conflicting unit definitions. This is foundational for multi-tenant systems where different workspaces may have different unit needs.

**Independent Test**: Add a new unit to the catalog, assign it to a product, and verify it's available for selection in inventory and invoice modules without requiring module-specific configuration.

**Acceptance Scenarios**:

1. **Given** an administrator role, **When** they create a new unit with name and symbol, **Then** the unit becomes immediately available across all modules
2. **Given** an existing unit, **When** an administrator updates its properties, **Then** the changes propagate to all modules using that unit
3. **Given** a unit is in use, **When** an administrator attempts to delete it, **Then** the system prevents deletion and shows where the unit is currently used

---

### User Story 4 - Track Inventory in Multiple Units (Priority: P2)

Inventory managers need to track stock levels in different units (e.g., stock in warehouse as pallets, but sell as individual items) with automatic conversion between units.

**Why this priority**: Warehouses often store products in bulk units while selling in smaller units. Automatic conversion reduces manual calculation errors and improves inventory accuracy.

**Independent Test**: Receive inventory in bulk units (e.g., pallets), sell in retail units (e.g., pieces), and verify inventory levels are accurately maintained with automatic conversion.

**Acceptance Scenarios**:

1. **Given** inventory stored in a base unit, **When** a sale occurs in a derived unit, **Then** the system automatically converts and deducts the correct quantity from inventory
2. **Given** multiple inventory locations with different storage units, **When** viewing total inventory, **Then** all quantities are normalized to a single unit for accurate totals
3. **Given** a product with unit conversions, **When** performing inventory adjustments, **Then** users can input adjustments in any defined unit

---

### Edge Cases

- What happens when a unit conversion is updated for a product that has existing orders or inventory transactions?
- How does the system handle circular unit conversions (e.g., A→B→C→A)?
- What happens when a user attempts to delete a unit that is referenced in historical invoices or orders?
- How does the system prevent duplicate unit names or symbols within the same workspace?
- What happens when unit conversion multipliers result in decimal quantities for items that should be whole numbers (e.g., pieces)?
- How are unit conversions handled for products that span multiple workspaces in a multi-tenant system?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a centralized unit module separate from product, inventory, and invoice modules
- **FR-002**: System MUST maintain all existing unit functionality after module extraction with no loss of features
- **FR-003**: System MUST support creating measurement units with properties: name, short name (symbol), and decimal precision
- **FR-004**: System MUST allow defining unit conversions with a base unit, derived unit, and conversion multiplier
- **FR-005**: System MUST support product-specific unit conversions where different products may have different conversion rules for the same unit pair
- **FR-006**: System MUST support inventory-specific unit conversions for warehouse management scenarios
- **FR-007**: System MUST ensure unit data is workspace-isolated in multi-tenant environments
- **FR-008**: System MUST provide APIs for other modules to access unit and conversion data without tight coupling
- **FR-009**: System MUST validate that unit conversions do not create circular dependencies
- **FR-010**: System MUST prevent deletion of units that are actively referenced by products, inventory, or invoices
- **FR-011**: System MUST maintain referential integrity when units are updated across all dependent modules
- **FR-012**: System MUST support querying all products or inventory items using a specific unit
- **FR-013**: System MUST provide unit conversion calculations as a service to other modules
- **FR-014**: System MUST migrate all existing unit data from the product module to the new unit module without data loss
- **FR-015**: System MUST update all references in product, inventory, and invoice modules to point to the new unit module

### Key Entities

- **Unit**: Represents a measurement unit with name, symbol, and decimal precision. Workspace-scoped for multi-tenancy.
- **Unit Conversion**: Defines conversion relationships between two units with a multiplier. Can be global (workspace-level) or context-specific (product-specific or inventory-specific).
- **Unit Reference**: Links from product, inventory, and invoice entities to the unit module, replacing current embedded unit definitions.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All existing unit functionality remains operational after module extraction with 100% feature parity
- **SC-002**: Unit data is accessible to product, inventory, and invoice modules through well-defined APIs with response times under 200ms
- **SC-003**: Zero data loss occurs during migration of unit and conversion data from product module to unit module
- **SC-004**: All automated tests pass after module extraction, confirming no breaking changes to dependent modules
- **SC-005**: Module extraction reduces code duplication by consolidating unit logic into a single module
- **SC-006**: System prevents data integrity violations with 100% validation coverage on unit references and conversions
- **SC-007**: Unit module follows the same architectural patterns as the tax module extraction, ensuring consistency in codebase structure

## Dependencies *(mandatory)*

### Module Dependencies

- **Tax Module**: Reference architecture for module extraction pattern, structure, and best practices
- **Core Module**: Provides base domain classes (OwnableBaseDomain) and multi-tenant support
- **Product Module**: Currently contains unit entities and logic that will be extracted
- **Inventory Module**: Contains inventory-specific unit conversion logic that depends on unit definitions
- **Invoice Module**: References units for line item pricing and quantity display

### Technical Dependencies

- Spring Boot framework for module configuration and dependency injection
- JPA/Hibernate for entity relationships between unit module and dependent modules
- Gradle build system for module dependency management

## Assumptions *(mandatory)*

1. The tax module extraction provides a proven pattern that can be replicated for the unit module
2. Unit conversions are directional (base → derived) and do not require bidirectional conversion logic at the entity level
3. Existing unit data in the product module is consistent and valid for migration
4. All modules (product, inventory, invoice) that reference units can be updated simultaneously to avoid partial migration states
5. The unit module will be deployed as part of the same application (not as a separate microservice)
6. Unit decimal precision is sufficient for all business use cases (no need for arbitrary precision arithmetic)
7. Multi-tenant isolation at the workspace level is sufficient for unit data (no sub-workspace unit isolation needed)

## Out of Scope *(mandatory)*

- Migration of historical data that references units in archived or deleted workspaces
- Support for custom unit systems beyond standard measurement units
- Real-time synchronization of unit changes across distributed systems
- User interface changes to unit selection components (frontend changes are out of scope)
- Performance optimization beyond basic query efficiency (no caching layer or advanced optimization)
- Unit localization or internationalization (e.g., different unit names for different languages)
- Integration with external unit conversion services or APIs
- Automated unit conversion suggestions based on industry standards

## Constraints *(mandatory)*

### Technical Constraints

- Module extraction must maintain backward compatibility with existing database schema
- Unit module must not introduce new external dependencies beyond what's already in the project
- All changes must follow existing Spring Boot module architecture and Kotlin coding standards
- Unit conversion calculations must use existing numeric types (Double) without introducing new libraries

### Business Constraints

- No downtime during module migration and deployment
- Existing API contracts with frontend applications cannot change
- Unit data must remain workspace-isolated to comply with multi-tenant security requirements
- Module extraction must be completed in a single release cycle to avoid partial migration issues

## Risks *(optional)*

### Technical Risks

- **Risk**: Circular dependencies between unit module and dependent modules during extraction
  **Mitigation**: Follow dependency injection patterns from tax module; use interface-based contracts

- **Risk**: Data migration failures when moving unit entities from product to unit module
  **Mitigation**: Implement idempotent migration scripts with rollback capability; test thoroughly in staging environment

- **Risk**: Performance degradation from cross-module queries after extraction
  **Mitigation**: Use JPA EntityGraph patterns to prevent N+1 queries; monitor query performance in testing

### Operational Risks

- **Risk**: Breaking changes to downstream modules during refactoring
  **Mitigation**: Comprehensive integration test suite must pass before deployment; staged rollout with feature flags

- **Risk**: Incomplete removal of unit code from source modules leaving duplicate logic
  **Mitigation**: Code review checklist to verify all unit-related code is removed or migrated; static analysis to detect unused code

## Related Features *(optional)*

- **Tax Module Extraction**: Provides the architectural blueprint and patterns for this unit module extraction
- **Product Module**: Current home of unit entities; will depend on unit module after extraction
- **Inventory Management**: Uses units for stock tracking and conversion; requires unit module for operation
- **Invoice Generation**: References units for line item display and calculations
- **Multi-Tenant Architecture**: Unit module must respect workspace isolation patterns

## Notes *(optional)*

### Implementation Notes

- The tax module extraction (reference: `/ampairs-backend/tax`) demonstrates the expected module structure:
  - Domain models (entities)
  - DTOs (Request/Response objects)
  - Repositories (data access layer)
  - Services (business logic)
  - Controllers (API endpoints)
  - Configuration classes

- Unit module should expose APIs at `/api/v1/unit` following existing REST conventions

- The unit module will handle:
  1. **Unit Management**: CRUD operations for measurement units
  2. **Conversion Management**: CRUD operations for unit conversions
  3. **Conversion Calculations**: Service methods for converting quantities between units
  4. **Reference Integrity**: Validation services to check unit usage before deletion

### Migration Strategy

- Phase 1: Create unit module structure with all dependencies
- Phase 2: Copy unit entities and logic to new module
- Phase 3: Update product, inventory, and invoice modules to depend on unit module
- Phase 4: Remove original unit code from product module
- Phase 5: Run full integration test suite and verify no regressions

### Data Migration Considerations

- Unit and UnitConversion entities are currently owned by the product module workspace
- Migration must preserve all workspace associations and ownership data
- Database foreign keys from product, inventory, and invoice tables must be validated
- Historical data in archived invoices must continue to reference unit information correctly