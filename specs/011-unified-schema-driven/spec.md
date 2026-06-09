# Feature Specification: Unified Schema-Driven Dynamic Forms

**Feature Branch**: `011-unified-schema-driven`
**Created**: 2026-06-09
**Status**: Draft
**Input**: User description: "Unified schema-driven dynamic forms across all domains — collapse the split field-config / custom-attribute model into one field definition, build a runtime form renderer that every domain uses, productize the admin editor, and standardize the configuration contract across customer, product, order, invoice, and business."

## Overview

Today, the application lets a workspace configure which fields appear on its data-entry screens (e.g. the customer form, the product form). That configuration exists but is barely connected to the actual screens: the real forms are still built by hand, so changing a field's setting often has no visible effect. The configuration itself is split into two unrelated concepts ("standard field settings" vs "custom field definitions") that behave inconsistently, the editing screen exposes raw technical details to business users, and deleting a field on one device does not reliably reach other devices.

This feature unifies that model into a single notion of a "form field", makes every domain's data-entry screen render directly from the workspace's configuration, and turns the configuration screen into a self-serve tool that business owners can actually use — with a live preview, drag-to-reorder, and guided validation rules instead of raw technical input.

## Clarifications

### Session 2026-06-09

- Q: Choice fields — static option lists only, or also dynamic options bound to live workspace data? → A: Both — a choice field's options may be a static admin-entered list OR bound to a named workspace data source (e.g. customer types, tax codes, units).
- Q: Complex fields the generic renderer can't express (image gallery, address block, map/location, business hours) — excluded, or delegated to native widgets? → A: Custom-widget escape hatch — such fields are still placed/ordered/gated by configuration, but their input control is supplied by the owning domain as a native widget; the whole form remains renderer-assembled.
- Q: Form sections — free-form label on each field, a separate first-class entity, or a fixed per-domain catalog? → A: First-class entity — sections are configurable records per entity type (own name, order, visibility) that fields reference, with their own configuration and sync lifecycle.
- Q: Migrate existing form configuration into the unified model? → A: No — this is a fresh setup. The unified store provisions empty and seeds defaults from the standard field registry; no legacy data is migrated or backfilled.
- Q: Maintain backward-compatible legacy form endpoints for older clients / the web app? → A: No — the Angular web client is deprecated and the app does a clean cutover to the unified feed. No legacy `/sync` adapters or legacy CRUD endpoints are retained.
- Q: Should the form schema be a DDD aggregate (Section owns Field), and what is the sync unit? → A: Yes — model it as a `FormSchema` aggregate (every field belongs to exactly one section; a default `General` section always exists; invariants enforced atomically on save) for the domain/editing/consistency layer, while keeping **row-level** distribution (sections + fields feeds) so concurrent admin edits still merge (FR-018 preserved).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Staff see the form their workspace configured (Priority: P1)

A staff member opens the customer entry screen. The fields they see, their labels, their order, which ones are required, and any extra custom fields the workspace added are exactly what the workspace administrator configured — not a hand-built screen that ignores the configuration. When the admin hides a field or marks it required, that change shows up on the entry screen.

**Why this priority**: This is the core promise of the whole feature. Without a screen that actually renders from configuration, every other part (unified model, admin editor) is configuration with no payoff. It is the minimum viable slice — proven on the customer domain first.

**Independent Test**: Configure the customer form so one standard field is hidden and one is marked required; open the customer entry screen and confirm the hidden field is absent and the required field blocks submission until filled. Add a custom field and confirm it appears and its value is saved.

**Acceptance Scenarios**:

1. **Given** an administrator has hidden the "GST number" field for customers, **When** a staff member opens the customer entry screen, **Then** the GST number field is not shown and is not required to save.
2. **Given** an administrator has marked "phone" as required, **When** a staff member tries to save a customer without a phone number, **Then** the form blocks submission and shows a clear inline message on that field.
3. **Given** an administrator has added a custom "Loyalty tier" choice field, **When** a staff member opens the customer entry screen, **Then** the field appears in its configured section and order, and the chosen value is saved with the customer.
4. **Given** a field has a validation rule (e.g. email format), **When** a staff member enters a value that breaks the rule, **Then** the form shows an inline error and prevents saving until corrected.

---

### User Story 2 - Administrators configure forms without technical knowledge (Priority: P1)

A workspace administrator opens the form configuration screen for an entity (e.g. customer). They can show/hide fields, mark fields required, reorder fields by dragging, group fields into sections, and add their own custom fields — all while seeing a live preview of the resulting entry form beside the controls. Advanced options (data type, validation rules) are available in a separate, guided area and never require typing raw technical syntax.

**Why this priority**: The admin experience is the second half of the value loop. A renderer that consumes configuration is only useful if non-technical owners can produce that configuration confidently. Equal priority to Story 1 because together they form the usable product.

**Independent Test**: As an administrator, hide a field, reorder two fields by dragging, add a custom dropdown field with three options, and confirm the live preview updates to match — then save and reopen to confirm persistence.

**Acceptance Scenarios**:

1. **Given** the form configuration screen, **When** the administrator toggles a field's visibility, **Then** the live preview immediately reflects the field appearing/disappearing.
2. **Given** several fields in a section, **When** the administrator drags a field to a new position, **Then** the new order is shown in the preview and retained after saving.
3. **Given** the administrator wants a choice field, **When** they add a custom field and enter its options through a guided control, **Then** no raw technical syntax is required and the preview shows a working dropdown.
4. **Given** the administrator sets a validation rule (e.g. "minimum length 10"), **When** they configure it, **Then** they pick from guided rule types and value inputs rather than entering code or expressions.
5. **Given** unsaved changes, **When** the administrator leaves the screen, **Then** they are warned that changes will be lost.

---

### User Story 3 - One consistent field model across the workspace (Priority: P2)

Standard fields (those backed by built-in data, like a customer's name) and custom fields (those a workspace invents, like "preferred metal") behave the same way in configuration and on the entry form. An administrator does not need to understand the difference between "field settings" and "attribute definitions" — there is one list of fields per form, each with the same set of controls.

**Why this priority**: Removes the conceptual split that makes the current system confusing and doubles maintenance. It is P2 because Stories 1 and 2 can ship on the unified model for the reference domain first; this story ensures the unification is complete and consistent rather than a parallel track.

**Independent Test**: On the configuration screen for one entity, confirm standard and custom fields appear in a single ordered list with identical controls (visibility, required, order, section, validation), and that reordering interleaves them freely.

**Acceptance Scenarios**:

1. **Given** the configuration screen, **When** the administrator views the field list, **Then** standard and custom fields are presented in one unified, freely-orderable list.
2. **Given** a standard field and a custom field, **When** the administrator edits each, **Then** both offer the same configuration controls where applicable.
3. **Given** a custom field's saved value on a record, **When** that record is viewed or edited, **Then** the value is preserved and shown exactly like a standard field's value.

---

### User Story 4 - Configuration changes sync everywhere and work offline (Priority: P2)

When an administrator changes a form's configuration, the change reaches every device in the workspace — including additions, edits, and deletions of fields. The entry forms continue to work when a device is offline, using the last-known configuration, and reconcile cleanly when connectivity returns. Deleting a field on one device removes it everywhere.

**Why this priority**: The current system cannot propagate deletions, which leads to "ghost" fields lingering on other devices. Reliable, offline-capable distribution is essential for a multi-device business, but depends on the unified model (Story 3) being in place.

**Independent Test**: Delete a custom field on device A; confirm it disappears from device B after sync. Make a configuration change while device B is offline; confirm the entry form still works and the change applies once device B reconnects.

**Acceptance Scenarios**:

1. **Given** a field is deleted on one device, **When** another device synchronizes, **Then** that field no longer appears in its configuration or entry forms.
2. **Given** a device is offline, **When** a staff member opens an entry form, **Then** the form renders from the most recently synced configuration without error.
3. **Given** simultaneous edits on two devices, **When** both synchronize, **Then** the system resolves to a single consistent configuration without losing unrelated changes.

---

### User Story 5 - Every domain uses the same form system (Priority: P3)

The customer, product, order, invoice, and business entry screens all render from the central configuration through the same mechanism. Adding configuration support to a new domain does not require building a bespoke form. A single source of truth defines which standard fields each domain offers, so configuration screens never list fields that don't exist or omit ones that do.

**Why this priority**: This is the standardization payoff and the long-term maintenance win, but it is a rollout that follows the reference implementation. Sequencing it last lets the model and renderer stabilize on one domain before being applied broadly.

**Independent Test**: For each target domain, confirm its entry screen renders from configuration and that the configuration screen lists exactly the standard fields that domain actually supports.

**Acceptance Scenarios**:

1. **Given** any target domain, **When** a staff member opens its entry screen, **Then** the screen is produced from configuration rather than a hand-built layout.
2. **Given** a domain's configuration screen, **When** an administrator views available standard fields, **Then** the list matches the fields that domain genuinely supports, with no missing or non-existent fields.
3. **Given** a new field is introduced to a domain in a future release, **When** the workspace next loads its configuration, **Then** the new field becomes available without erasing the workspace's existing customizations.

---

### Edge Cases

- **Deleting a field that has stored data**: When a custom field is removed, previously captured values for that field are retained in storage but no longer shown or editable, so historical records are not silently destroyed.
- **Standard field tied to required data**: A field that is structurally essential to a record (e.g. the field that identifies the record) cannot be hidden or made optional; the system prevents the configuration that would break record integrity.
- **Unknown or unsupported entity type**: If a configuration references an entity type the application does not support, the configuration screen surfaces this clearly instead of failing silently.
- **Validation rule that no value can satisfy**: If an administrator sets contradictory rules (e.g. minimum length greater than maximum length), the configuration screen warns before saving.
- **Conflicting offline edits**: When the same field is edited on two devices while offline, reconciliation produces one consistent result and never leaves a half-applied configuration.
- **Required custom field added after records exist**: Existing records missing the newly-required value are not retroactively invalidated; the requirement applies to new edits going forward.
- **Empty configuration**: When a workspace has never customized a form, the entry screen renders a sensible default set of fields and sections derived from the domain's standard fields.
- **Deleting a non-empty section**: When a section that still contains fields is removed, the configuration screen requires the administrator to first reassign those fields to another section (or the system reassigns them to a default/unsectioned group) so no field is left orphaned or hidden unintentionally.

## Requirements *(mandatory)*

### Functional Requirements

**Unified field model**

- **FR-001**: The system MUST represent every configurable form field — whether backed by built-in data or workspace-defined — as a single kind of "field", distinguished only by whether its value is stored as built-in data or as workspace-added data.
- **FR-002**: Each field MUST carry: a stable identifier, a display label, a data type, whether it is built-in or custom, visibility, required, and enabled states, a section grouping, a display order, an optional default value, and optional validation rules.
- **FR-003**: The system MUST support these field data types at minimum: single-line text, multi-line text, number, true/false, date, and single-choice selection.
- **FR-003a**: A single-choice field's options MUST be definable EITHER as a static list entered by the administrator OR as a binding to a named live workspace data source (e.g. customer types, tax codes, units), so the renderer can reproduce existing dynamic dropdowns. The administrator selects the option source through guided controls without entering technical syntax.
- **FR-004**: Validation rules MUST be expressed as structured, typed rules (e.g. required, text length range, numeric range, allowed format such as email/phone, allowed choice set) — not as free-form technical expressions entered by administrators.

**Form rendering (runtime)**

- **FR-005**: Each domain's data-entry screen MUST be produced from the workspace's current field configuration for that entity, rather than from a hand-built layout.
- **FR-006**: The rendered form MUST show only visible fields, in their configured order, grouped under their configured sections.
- **FR-007**: The rendered form MUST enforce required fields and validation rules at the point of entry, showing inline, field-level error messages and blocking save until resolved.
- **FR-008**: The rendered form MUST read existing values and write entered values for both built-in and custom fields transparently, so the staff member experiences no difference between the two.
- **FR-008a**: The renderer MUST support a "custom widget" field type for inputs the generic types cannot express (e.g. image gallery, structured address, location/map, business hours). Such fields' visibility, required state, order, and section MUST remain driven by configuration, while their input control is provided by the owning domain. The complete form MUST still be assembled by the renderer (no domain appends hand-built fields outside it).
- **FR-009**: The rendered form MUST function using the last-synced configuration when the device is offline.

**Admin configuration experience**

- **FR-010**: Administrators MUST be able to toggle a field's visibility and required state, change its label and default value, assign it to a section, and reorder fields (including by dragging).
- **FR-010a**: Sections MUST be first-class configurable records per entity type, each with its own name, display order, and visibility. Administrators MUST be able to create, rename, reorder, hide, and remove sections, and assign fields to them. Section changes MUST distribute and sync across devices on the same lifecycle as field changes (additions, edits, deletions, offline support).
- **FR-011**: Administrators MUST be able to add, edit, and remove custom fields for an entity.
- **FR-012**: The configuration screen MUST present a live preview of the resulting entry form that updates as changes are made.
- **FR-013**: Advanced settings (data type and validation rules) MUST be presented through guided controls, separated from everyday settings, and MUST NOT require entering raw technical syntax.
- **FR-014**: The system MUST warn administrators before discarding unsaved configuration changes.
- **FR-015**: The system MUST prevent configurations that would compromise record integrity (e.g. hiding or removing a structurally essential built-in field).

**Distribution, sync, and lifecycle**

- **FR-016**: Configuration changes — including additions, edits, and deletions — MUST propagate to every device in the workspace.
- **FR-017**: Deleting a field MUST remove it on all devices while retaining any previously stored values for removed custom fields.
- **FR-018**: The system MUST resolve concurrent multi-device configuration edits into a single consistent result without losing unrelated changes.
- **FR-019**: Configuration MUST be scoped per workspace, so one workspace's customizations never affect another.

**Standardization across domains**

- **FR-020**: The set of built-in (standard) fields available for each domain MUST come from a single authoritative source, so configuration screens list exactly the fields that domain supports.
- **FR-021**: The system MUST NOT maintain duplicate, independently-edited lists of default fields in more than one place.
- **FR-022**: Introducing a new built-in field to a domain in a later release MUST make that field available to workspaces without erasing their existing customizations.
- **FR-023**: The customer, product, order, invoice, and business domains MUST all render from and be configurable through this unified system, with customer serving as the reference implementation delivered first.
- **FR-024**: Entity types MUST be referenced through a defined, validated set of values rather than free-form text, so unsupported entities are detected rather than silently accepted.

**Provisioning & rollout**

- **FR-025**: This is a fresh setup — the unified configuration store provisions empty and derives each workspace's initial fields and sections from the standard field registry on first access. No legacy form data is migrated or backfilled.
- **FR-026**: Rollout MUST be incremental domain by domain (customer first), and the unified configuration feed MUST work for a domain as soon as it is added without requiring all domains to be migrated together. No backward-compatible legacy endpoints are retained (the web client is deprecated; the app cuts over cleanly to the unified feed).

### Key Entities *(include if feature involves data)*

- **Field Definition**: A single configurable field on an entity's form. Holds identity, label, data type, origin (built-in vs custom), visibility/required/enabled flags, section, order, default value, and validation rules. Replaces the two prior separate concepts.
- **Validation Rule**: A structured constraint attached to a field (e.g. required, length range, numeric range, format, allowed choices), interpretable both when an administrator configures it and when a staff member fills the form.
- **Choice Option Source**: For single-choice fields, the origin of the selectable options — either a static list of values defined by the administrator, or a reference to a named live workspace data source whose entries populate the choices at render time.
- **Form Section**: A first-class configurable record, per entity type, that groups fields into a logical block. Holds its own name, display order, and visibility, and has its own configuration and sync lifecycle (created, renamed, reordered, hidden, removed). Fields reference the section they belong to.
- **Entity Configuration Schema**: The complete, ordered set of field definitions (and their sections) for one entity type within one workspace — the unit that is distributed to devices and rendered.
- **Entity Type**: The defined, validated set of supported domains (customer, product, order, invoice, business) that a configuration can target.
- **Standard Field Registry**: The single authoritative source describing which built-in fields each domain offers, used to generate defaults and validate configurations.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of entry screens in the target domains (customer, product, order, invoice, business) are produced from workspace configuration, with zero hand-built form layouts remaining for those domains.
- **SC-002**: A configuration change made by an administrator (show/hide, reorder, add custom field, mark required) is reflected on a staff member's entry screen on another device within one normal sync cycle, including deletions.
- **SC-003**: An administrator can hide a field, add a custom choice field, and reorder fields, then verify the result via live preview, in under 2 minutes and without entering any raw technical syntax.
- **SC-004**: There is exactly one source of truth for each domain's built-in fields; an audit finds no duplicated default-field lists.
- **SC-005**: Field deletions propagate to all devices in 100% of cases (eliminating the current inability to remove fields across devices).
- **SC-006**: Entry forms render correctly from the last-synced configuration while offline in 100% of tested offline scenarios.
- **SC-007**: Following rollout, the amount of bespoke per-domain form code is reduced by at least 70% relative to before, measured across the target domains.
- **SC-008**: A workspace that has never customized a form sees a complete, sensible default form (all registry-defined standard fields, correctly sectioned) on first use, with no manual setup required.

## Assumptions

- **Who may configure forms**: Form configuration is restricted to workspace administrators/owners; ordinary staff can fill forms but not change configuration. (Existing workspace roles govern this.)
- **Standard fields cannot be deleted, only hidden/disabled**: Because built-in fields map to structural data, administrators may hide, disable, reorder, relabel, or make them optional/required, but only custom fields may be deleted outright.
- **Deleted custom field values are retained, not purged**: Removing a custom field hides it but preserves historical values, so deletion is non-destructive to existing records.
- **Conflict resolution favors local unsynced edits during reconciliation**, consistent with the application's established offline behavior.
- **Default configuration is derived automatically** from each domain's standard field registry when a workspace has not customized a form.
- **The existing offline distribution mechanism and per-workspace scoping are retained**; this feature changes the configuration model and the screens, not the underlying distribution approach.
- **The reference rollout order** is customer first, then product, order, invoice, and business.

## Dependencies

- Each target domain (customer, product, order, invoice, business) must expose its built-in field set to the standard field registry.
- The workspace role/permission model is the source of who may edit configuration.
- The existing per-workspace, offline-capable configuration distribution is the transport for schema changes.

## Out of Scope

- Conditional/branching fields (show field B only when field A has a certain value).
- Computed or formula fields whose values derive from other fields.
- Multi-select choice fields. (Complex inputs such as image gallery, structured address, location/map, and business hours are supported via the custom-widget escape hatch of FR-008a — they are configurable and renderer-placed, but their input controls are domain-provided and are not reimplemented generically by this feature.)
- Per-user or per-role form variations within a single workspace (configuration is per workspace, per entity).
- Form configuration for domains outside the five named targets.
- Public/external-facing form rendering (e.g. customer-facing web forms) — this feature concerns internal staff entry screens.
