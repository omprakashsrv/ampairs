# Quickstart: Putting a Domain on the Unified Form System

How to make a domain (e.g. `product`) configurable and schema-driven once the core (`form` module +
`feature/form-api` renderer) is in place. Customer is the reference; follow the same steps.

---

## Backend — declare the domain's standard fields (single source of truth)

1. In the domain module (e.g. `product/src/main/kotlin/com/ampairs/product/domain/service/`), implement
   the registry SPI:

   ```kotlin
   @Component
   class ProductStandardFieldProvider : StandardFieldProvider {
       override fun entityType() = EntityType.PRODUCT
       override fun standardFields() = listOf(
           StandardFieldSpec(key = "name",   label = "Name",  dataType = TEXT,   section = "Basics",
                             essential = true, order = 0, validation = listOf(Required)),
           StandardFieldSpec(key = "sku",    label = "SKU",   dataType = TEXT,   section = "Basics", order = 1),
           StandardFieldSpec(key = "taxCode",label = "Tax",   dataType = CHOICE, section = "Pricing", order = 5,
                             optionSource = DYNAMIC, dynamicSourceKey = "tax_codes"),
           // ...
       )
   }
   ```

2. That's it server-side. `FormFieldRegistry` auto-discovers the `@Component`; the `form` service seeds
   defaults, merges new fields on read (non-destructive), and validates STANDARD field keys. **Delete any
   hardcoded seeding for this entity** from the old `ConfigService`.

3. CUSTOM values still land in the entity's existing `attributes: Map<String, Any>` JSON column — no
   schema change to the domain entity.

---

## App — render the form through `DynamicFormRenderer`

1. Replace the hand-built screen body with:

   ```kotlin
   val schema by viewModel.schema.collectAsStateWithLifecycle()      // ConfigLookup.observeConfigSchema(PRODUCT)
   val formState = rememberFormValueState(schema, initialValues = product.toValueMap())

   DynamicFormRenderer(
       schema = schema,
       state  = formState,                 // two-way binding + inline validation
   )
   // on save: if (formState.validateAll()) repository.save(product.applyValues(formState.values))
   ```

2. **Standard fields** bind to entity columns via `toValueMap()` / `applyValues()`; **custom fields** map
   to the `attributes` map. The renderer handles text/number/date/boolean/choice automatically.

3. **Dynamic choice options** — register a provider for each `dynamic_source_key` this domain uses:

   ```kotlin
   @ContributesIntoMap(WorkspaceScope::class) @OptionSourceKey("tax_codes") @Inject
   class TaxCodeOptionProvider(private val repo: TaxRepository) : DynamicOptionProvider {
       override fun options(): Flow<List<Option>> =
           repo.observeTaxCodes().map { it.map { t -> Option(t.uid, t.displayName) } }
   }
   ```

4. **Custom widgets** — for `data_type = CUSTOM` fields (image gallery, address, map, hours), register the
   native composable:

   ```kotlin
   @ContributesIntoMap(WorkspaceScope::class) @WidgetKey("image_gallery") @Inject
   class ImageGalleryWidget(...) : CustomFieldWidget {
       @Composable override fun Render(field: FormField, value: FieldValue, onChange: (FieldValue) -> Unit) { ... }
   }
   ```

   The renderer places/orders/gates it from config; the widget owns only the input control.

5. Wire navigation `onFormConfig = { backStack.add(Route.FormConfig("product")) }` (already exists for
   customer/product/business; add for order/invoice).

---

## Verify

- **Config drives the form**: hide a field / mark required / reorder / add a custom choice field in the
  admin editor → confirm the live preview and the real entry screen reflect it (SC-001/002/003).
- **Deletes propagate**: delete a custom field on device A → it disappears on device B after one sync
  cycle (SC-005).
- **Offline**: airplane mode → entry screen still renders from last-synced schema (SC-006).
- **No duplicate defaults**: grep confirms the domain's fields exist only in its `StandardFieldProvider`
  (SC-004) — no hardcoded list in `ConfigService` or app `DefaultFormConfigs`.

### Compile gates (app, after any commonMain change)
```bash
./gradlew :feature:form:check
./gradlew androidApp:compileDebugKotlinAndroid
./gradlew shared:compileKotlinIosSimulatorArm64
./gradlew desktopApp:compileKotlin
```

### Backend
```bash
./gradlew :ampairs_service:flywayInfo     # pick next migration version
./gradlew :form:test && ./gradlew ciBuild
```


## Validation notes (SC-007, recorded 2026-06-10)

Qualitative outcome — bespoke form code retired by the unified system:
- Backend: the entire legacy form stack (`FieldConfig` + `AttributeDefinition` entities, DTOs,
  repositories, seeding `ConfigService`, and five legacy endpoints) was deleted; defaults now flow
  exclusively through five `StandardFieldProvider`s + `FormFieldRegistry`.
- App: `DefaultFormConfigs` deleted; the form-config editor, customer entry form (standard +
  custom fields), product attributes, and the business custom-attributes screen all render from the
  synced `FormSchema` through one renderer + one shared `ConfigAttributesSection`.
- Customer/product custom values round-trip Room + `/sync` end-to-end (verified on device);
  optimistic-version conflicts self-heal (regression-tested in both repos).
- Not yet on the renderer: order/invoice entry bodies (transactional document builders pending an
  `attributes` column end-to-end) and the customer form's bespoke address/autocomplete controls
  (kept deliberately, fed by the same schema).
