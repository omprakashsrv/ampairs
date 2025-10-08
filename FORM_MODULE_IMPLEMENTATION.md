# Form Module Implementation Summary

## ✅ Completed - Generic Form Configuration System

### **Architecture Overview**

A dedicated `form` module created for both backend and mobile to provide **centralized, entity-agnostic form configuration** using a single `entity_type` field to differentiate between customer, product, inventory, and other forms.

---

## **Backend Implementation** (`/ampairs-backend/form/`)

### **1. Domain Models**

#### **FieldConfig.kt**
- JPA entity for standard field configurations
- Fields: `entityType`, `fieldName`, `displayName`, `visible`, `mandatory`, `enabled`, `displayOrder`, `validationType`, `validationParams`, `placeholder`, `helpText`, `defaultValue`
- Multi-tenant with `@TenantId` on `workspaceId`
- UID prefix: `FC`

#### **AttributeDefinition.kt**
- JPA entity for custom attribute definitions
- Fields: `entityType`, `attributeKey`, `displayName`, `dataType`, `visible`, `mandatory`, `enabled`, `displayOrder`, `category`, `defaultValue`, `validationType`, `validationParams`, `enumValues`, `placeholder`, `helpText`
- Multi-tenant with `@TenantId` on `workspaceId`
- UID prefix: `AD`

### **2. DTOs**

- `FieldConfigResponse` / `FieldConfigRequest` - Snake_case JSON mapping
- `AttributeDefinitionResponse` / `AttributeDefinitionRequest` - Snake_case JSON mapping
- `EntityConfigSchemaResponse` - Combined schema response
- Extension functions for entity ↔ DTO conversion

### **3. Repositories**

- `FieldConfigRepository` - Spring Data JPA with tenant filtering
- `AttributeDefinitionRepository` - Spring Data JPA with tenant filtering
- Methods: `findByEntityTypeOrderByDisplayOrderAsc()`, `findByEntityTypeAndFieldName()`, etc.

### **4. Service Layer**

**ConfigService.kt**
- `getConfigSchema(entityType)` - Get complete schema for entity type
- `getAllConfigSchemas()` - Get all schemas (admin)
- `saveFieldConfig()` - Create/update field config
- `saveAttributeDefinition()` - Create/update attribute definition
- `deleteFieldConfig()` - Delete field config
- `deleteAttributeDefinition()` - Delete attribute definition

### **5. Controller**

**ConfigController.kt** - `/api/v1/form`
```
GET  /api/v1/form/schema?entity_type=customer
GET  /api/v1/form/schemas
POST /api/v1/form/field-config
POST /api/v1/form/attribute-definition
DELETE /api/v1/form/field-config?entity_type=X&field_name=Y
DELETE /api/v1/form/attribute-definition?entity_type=X&attribute_key=Y
```

### **6. Build Configuration**

- Added to `settings.gradle.kts`
- Added to `ampairs_service/build.gradle.kts` dependencies
- Proper Kotlin/Spring Boot/JPA setup with repositories
- bootJar disabled (library module)

---

## **Mobile Implementation** (`/ampairs-mp-app/.../form/`)

### **1. Domain Models**

**Constants:**
- `EntityType.kt` - Entity type constants (CUSTOMER, PRODUCT, INVENTORY, etc.)
- `AttributeDataType.kt` - Data type constants (STRING, NUMBER, BOOLEAN, etc.)
- `AttributeValidationType.kt` - Validation types (REGEX, LENGTH, RANGE, etc.)
- `ValidationParamKeys.kt` - Validation parameter keys

**Data Classes:**
- `EntityFieldConfig.kt` - Field configuration with snake_case serialization
- `EntityAttributeDefinition.kt` - Attribute definition with snake_case serialization
- `EntityConfigSchema.kt` - Combined schema with helper methods:
  - `getVisibleFields()`, `getMandatoryFields()`
  - `getVisibleAttributes()`, `getMandatoryAttributes()`
  - `getAttributesByCategory()`
  - `isFieldVisible()`, `isFieldMandatory()`
  - `isAttributeVisible()`, `isAttributeMandatory()`

### **2. Data Layer**

**ConfigApi.kt / ConfigApiImpl.kt**
- `getConfigSchema(entityType)` - Fetch schema from backend
- `getAllConfigSchemas()` - Fetch all schemas (admin)
- Uses Ktor HTTP client with `ApiUrlBuilder.formUrl()`

**ConfigRepository.kt**
- Caching by entity type using `MutableStateFlow<Map<String, EntityConfigSchema>>`
- `getConfigSchema(entityType)` - Fetch and cache
- `observeConfigSchema(entityType)` - Reactive Flow
- `refreshConfig(entityType)` - Force refresh
- `preloadConfigs(entityTypes)` - Batch preload
- `clearCache(entityType)` / `clearAllCache()`

### **3. Dependency Injection**

**FormModule.kt**
```kotlin
val formModule = module {
    single<ConfigApi> { ConfigApiImpl(get()) }
    single<ConfigRepository> { ConfigRepository(get()) }
}
```

**Koin.kt** - Added `formModule` to initialization

### **4. Infrastructure**

**ApiUrlBuilder.kt**
- Added `formUrl(path)` function for `/api/v1/form` endpoints

---

## **Database Schema**

### **field_config Table**
```sql
CREATE TABLE field_config (
    uid VARCHAR(32) PRIMARY KEY,
    workspace_id VARCHAR(32) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,  -- 'customer', 'product', etc.
    field_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    visible BOOLEAN DEFAULT TRUE,
    mandatory BOOLEAN DEFAULT FALSE,
    enabled BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,
    validation_type VARCHAR(50),
    validation_params JSON,
    placeholder VARCHAR(255),
    help_text TEXT,
    default_value VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,

    UNIQUE(workspace_id, entity_type, field_name),
    INDEX idx_workspace_entity (workspace_id, entity_type)
);
```

### **attribute_definition Table**
```sql
CREATE TABLE attribute_definition (
    uid VARCHAR(32) PRIMARY KEY,
    workspace_id VARCHAR(32) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    attribute_key VARCHAR(100) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    data_type VARCHAR(50) DEFAULT 'STRING',
    visible BOOLEAN DEFAULT TRUE,
    mandatory BOOLEAN DEFAULT FALSE,
    enabled BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,
    category VARCHAR(100),
    default_value VARCHAR(255),
    validation_type VARCHAR(50),
    validation_params JSON,
    enum_values JSON,
    placeholder VARCHAR(255),
    help_text TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,

    UNIQUE(workspace_id, entity_type, attribute_key),
    INDEX idx_workspace_entity (workspace_id, entity_type)
);
```

---

## **Usage Example**

### **Mobile ViewModel**
```kotlin
class CustomerFormViewModel(
    private val configRepository: ConfigRepository
) : ViewModel() {

    val configSchema = configRepository
        .observeConfigSchema(EntityType.CUSTOMER)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            configRepository.getConfigSchema(EntityType.CUSTOMER)
        }
    }

    fun isFieldVisible(fieldName: String): Boolean =
        configSchema.value?.isFieldVisible(fieldName) ?: true

    fun isFieldMandatory(fieldName: String): Boolean =
        configSchema.value?.isFieldMandatory(fieldName) ?: false
}
```

### **Backend Service**
```kotlin
val schema = configService.getConfigSchema("customer")
// Returns EntityConfigSchemaResponse with all field configs and attributes
```

---

## **Key Features**

✅ **Single Source of Truth** - One API, one repository, one set of tables
✅ **Entity Type Filtering** - `entity_type` field differentiates configs
✅ **Multi-Tenant** - Automatic workspace isolation via `@TenantId`
✅ **Caching** - Efficient client-side caching by entity type
✅ **Reactive** - Flow-based reactive updates
✅ **Type-Safe** - Kotlin domain models with proper serialization
✅ **Scalable** - Add new entity types without code changes
✅ **Admin-Friendly** - CRUD operations for all configs

---

## **Next Steps**

1. **Database Migration** - Create Flyway migration script
2. **Seed Data** - Add initial customer field configs
3. **Customer Module Migration** - Update `CustomerFormViewModel` to use `ConfigRepository`
4. **UI Components** - Create generic `ConfigurableFormField` and `DynamicAttributeField`
5. **Product Module** - Add `ProductFieldNames` and reuse same system
6. **Testing** - Unit tests for repository, service, and API layers
7. **Documentation** - API documentation with Swagger/OpenAPI

---

## **Build Status**

- ✅ Backend form module builds successfully
- ✅ Mobile form module created (Kotlin compiles with Koin integration)
- ⚠️ Compiler warnings for `@JsonProperty` (future compatibility - safe to ignore)

---

**Generated:** October 2, 2025
**Author:** Claude Code
