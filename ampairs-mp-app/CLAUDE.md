# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Ampairs Mobile Application** is a Kotlin Multiplatform business management client that integrates with the Ampairs Spring Boot backend system. It targets **Android, iOS, and Desktop (JVM)** platforms using **Compose Multiplatform** with an **offline-first architecture**.

### **🏗️ System Integration**

This mobile app is part of a **three-tier Ampairs ecosystem**:

1. **Backend (Spring Boot + Kotlin)** - `/ampairs_service` + domain modules  
2. **Web Frontend (Angular + Material Design 3)** - `/ampairs-web`
3. **Mobile App (Kotlin Multiplatform)** - `/ampairs-mp-app` ← **THIS PROJECT**

**Backend Integration**: Consumes REST APIs from Spring Boot backend with JWT authentication, multi-tenant support, and offline-first synchronization.

## Architecture

### **🔄 Offline-First Architecture with Store5**

The app implements **Store5 pattern** for robust offline-first data management:

```
┌─────────────────────────────────────────────────────────────────┐
│                    Presentation Layer                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │   Compose   │  │     MVI     │  │  UI State   │             │
│  │ Multiplatform │  │  Actions   │  │ Management  │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────┐
│                   Store5 Layer                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │   Store     │  │  Fetcher    │  │ SourceOfTruth│            │
│  │ Controller  │  │ (Network)   │  │ (Local DB)   │            │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────┐
│                   Data Layer                                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │    Room     │  │    Ktor     │  │   Cache     │             │
│  │  Database   │  │ HTTP Client │  │ Management  │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────┬───────────────────────────────────────────┘
                      │ REST API Calls
┌─────────────────────▼───────────────────────────────────────────┐
│              Spring Boot Backend                                │
│           (JWT Auth + Multi-tenancy)                           │
└─────────────────────────────────────────────────────────────────┘
```

### **Technology Stack**

- **UI Framework**: Jetpack Compose Multiplatform with Material 3 Design System
- **Dependency Injection**: Koin with modular setup per feature
- **Local Database**: Room Database (replaces SQLDelight) with platform-specific drivers
- **Offline-First**: Store5 for caching, synchronization, and conflict resolution
- **HTTP Client**: Ktor with automatic JWT token refresh and bearer authentication
- **Navigation**: Androidx Navigation Compose with type-safe routing
- **State Management**: MVI pattern with ViewModels and Resource<T> wrappers
- **Image Loading**: Custom image loader with caching support
- **Serialization**: kotlinx.serialization for JSON parsing
- **Background Sync**: Platform-specific background task scheduling

### **Platform Support**

- **Android**: Native Android app with Room SQLite, background sync via WorkManager
- **iOS**: Native iOS app with Core Data integration, background refresh capabilities
- **Desktop (JVM)**: Desktop application with JDBC drivers, timer-based sync coordination

### **Module Structure**

```
ampairs-mp-app/
├── composeApp/                    # Main application module
│   ├── src/commonMain/kotlin/
│   │   ├── com.ampairs.auth/      # Authentication & user management
│   │   ├── com.ampairs.workspace/ # Multi-tenant workspace management
│   │   ├── com.ampairs.customer/  # Customer relationship management
│   │   ├── com.ampairs.product/   # Product catalog & inventory
│   │   ├── com.ampairs.order/     # Order processing & management
│   │   ├── com.ampairs.invoice/   # Invoice generation & management
│   │   └── com.ampairs.tally/     # ERP system integration
│   ├── src/androidMain/kotlin/    # Android-specific implementations
│   ├── src/iosMain/kotlin/        # iOS-specific implementations (commented out)
│   └── src/desktopMain/kotlin/    # Desktop-specific implementations
├── core/                          # Cross-cutting concerns
├── common/                        # Shared UI components and utilities
└── shared/                        # Business logic and data models
```

## **🔑 Key Development Patterns**

### **Store5 Implementation Pattern**

Each domain module implements the Store5 pattern for offline-first data access:

```kotlin
class CustomerStore {
    private val store = StoreBuilder
        .from(
            fetcher = Fetcher.of { key: CustomerKey ->
                // Network call to Spring Boot API
                customerApi.getCustomers(
                    tenantId = key.tenantId,
                    page = key.page,
                    size = key.size
                )
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { key: CustomerKey ->
                    // Read from Room database
                    customerRepository
                        .getCustomers(key.tenantId, key.page, key.size)
                        .asFlow()
                },
                writer = { key: CustomerKey, customers: List<Customer> ->
                    // Write to Room database
                    customerRepository.insertOrReplaceCustomers(customers)
                }
            )
        )
        .build()
        
    fun stream(key: CustomerKey): Flow<StoreReadResponse<List<Customer>>> = 
        store.stream(StoreReadRequest.cached(key, refresh = false))
}
```

### **Room Database Architecture**

- **Platform-specific databases**: Room for Android, Core Data bridge for iOS, JDBC for Desktop
- **Multi-tenant isolation**: All entities include tenant_id for proper data segregation
- **Offline sync metadata**: Entities track sync state, last sync time, and pending changes
- **Automatic migrations**: Room handles schema migrations across app updates

```kotlin
@Entity(tableName = "customer")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val name: String,
    val email: String?,
    val phone: String?,
    // Address fields
    val addressLine1: String?,
    val addressLine2: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val pincode: String?,
    val gstNumber: String?,
    // Sync metadata
    val syncStatus: String = "SYNCED", // SYNCED, PENDING_UPLOAD, PENDING_DELETE
    val createdAt: Long,
    val updatedAt: Long,
)
```

### **Authentication & Multi-tenancy**

- **Backend Integration**: Follows same JWT authentication as Spring Boot backend
- **Phone + OTP Flow**: Matches backend authentication endpoint patterns
- **Multi-device Support**: JWT tokens include device_id for concurrent session management
- **Tenant Context**: HTTP headers automatically include workspace/company context
- **Token Storage**: Secure token storage using Room database with encryption

### **API Integration with Backend**

```kotlin
// Matches backend API patterns exactly
interface CustomerApi {
    @GET("/customer/v1/list")
    suspend fun getCustomers(
        @Header("X-Company-ID") companyId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sortBy") sortBy: String = "createdAt",
        @Query("sortDir") sortDir: String = "desc"
    ): Response<PagedCustomerResponse>
    
    @POST("/customer/v1")
    suspend fun createCustomer(
        @Header("X-Company-ID") companyId: String,
        @Body request: CreateCustomerRequest
    ): Response<CustomerApiModel>
}
```

### **Dependency Injection with Koin**

- **Feature-based modules**: Each domain has its own `FeatureModule.kt`
- **Platform-specific dependencies**: Android, iOS, Desktop implementations
- **ViewModel injection pattern**: `val viewModel: MyViewModel = koinInject { parametersOf(id) }`
- **Repository pattern**: Clean separation between API, database, and domain layers

```kotlin
val customerModule = module {
    // API layer
    single<CustomerApi> { CustomerApiImpl(get()) }
    
    // Database layer
    single<CustomerDao> { get<AppDatabase>().customerDao() }
    single<CustomerRepository> { OfflineFirstCustomerRepository(get(), get(), get()) }
    
    // Store5 integration
    single { CustomerStore(get(), get()) }
    
    // ViewModels
    factory { (customerId: String?) -> CustomerDetailsViewModel(customerId, get(), get()) }
    factory { CustomerListViewModel(get(), get()) }
}
```

### **Navigation with Type Safety**

```kotlin
// Type-safe navigation routes
@Serializable sealed interface Route {
    @Serializable data object CustomerList : Route
    @Serializable data class CustomerDetails(val customerId: String) : Route
    @Serializable data class ProductDetails(val productId: String) : Route
    @Serializable data class OrderView(val orderId: String) : Route
}

// Usage in Compose
NavHost(navController, startDestination = Route.CustomerList) {
    composable<Route.CustomerList> {
        CustomerListScreen(
            onCustomerClick = { customerId ->
                navController.navigate(Route.CustomerDetails(customerId))
            }
        )
    }
    
    composable<Route.CustomerDetails> { backStackEntry ->
        val args = backStackEntry.toRoute<Route.CustomerDetails>()
        CustomerDetailsScreen(customerId = args.customerId)
    }
}
```

## **Build Commands**

### **Android**
```bash
./gradlew composeApp:assembleDebug
./gradlew composeApp:installDebug
```

### **Desktop**
```bash
./gradlew composeApp:run                    # Run desktop app
./gradlew composeApp:package               # Create native distributions
```

### **iOS** (Currently commented out)
```bash
./gradlew composeApp:embedAndSignAppleFrameworkForXcode
```

### **Cleanup**
```bash
./gradlew clean
./cleanup.sh  # Remove IDE files and build artifacts
```

## **Version Catalog**

Current versions from `gradle/libs.versions.toml`:

- **Kotlin**: 2.2.0  
- **Compose Multiplatform**: 1.8.2
- **Room**: 2.7.0-alpha11 (replaces SQLDelight)
- **Store5**: 5.1.0 (offline-first architecture)
- **Ktor**: 3.2.1
- **Koin**: 4.1.0
- **Kotlinx Serialization**: 1.7.3

## **Development Environment**

- **IDE**: Android Studio with Kotlin Multiplatform plugin
- **Environment Check**: Use `kdoctor` to validate multiplatform setup
- **Android**: Min SDK 24, Target SDK 35, Compile SDK 36
- **Desktop**: Java 21+ required
- **iOS**: Xcode 15+ (when iOS target is enabled)

## **Business Domain Integration**

The app provides complete feature parity with the backend system:

### **Core Features**
- **Authentication**: Phone/OTP login with JWT tokens and multi-device support
- **Workspace Management**: Multi-tenant workspace selection and configuration  
- **Customer Management**: Comprehensive CRM with address handling and GST compliance
- **Product Catalog**: Product management with categories, tax codes, and image storage
- **Inventory Management**: Stock tracking, movement reporting, and low-stock alerts
- **Order Processing**: Order creation, management, status workflows, and pricing
- **Invoice Generation**: Invoice creation, GST compliance, PDF generation, email delivery
- **Tally Integration**: ERP system synchronization and data exchange

### **Data Flow & Synchronization**

1. **Offline-first Operations**: All CRUD operations work offline and sync automatically
2. **Conflict Resolution**: Store5 handles data conflicts with last-write-wins strategy
3. **Background Sync**: Platform-specific background tasks ensure data consistency
4. **Real-time Updates**: WebSocket connections for live data updates (when online)
5. **Bulk Operations**: Efficient batch processing for large data sets

## **Platform-Specific Implementations**

### **Android**
- **Database**: Room with SQLite driver
- **Background Sync**: WorkManager for periodic and network-triggered sync
- **Notifications**: Local notifications for sync status and business alerts
- **File Storage**: Android-specific external storage for documents and images

### **iOS** 
- **Database**: Room with Core Data bridge (when iOS target is enabled)
- **Background Sync**: Background App Refresh for data synchronization
- **Notifications**: iOS push notifications and local alerts
- **File Storage**: iOS document directory for secure file management

### **Desktop (JVM)**
- **Database**: Room with JDBC drivers (H2 or SQLite)
- **Background Sync**: Timer-based coordination and network state monitoring
- **File Storage**: Platform-specific app data directories
- **Window Management**: Native window controls and system integration

## **Recent Updates & Migration Notes**

### **Database Migration (Room replacing SQLDelight)**
- ✅ **Completed**: Full migration from SQLDelight to Room database
- ✅ **Store5 Integration**: Offline-first architecture with automatic conflict resolution
- ✅ **Cross-platform**: Room works consistently across Android, iOS, and Desktop
- ✅ **Migration Scripts**: Automatic data migration from old SQLDelight schemas

### **Store5 Implementation (January 2025)**
- ✅ **Offline-first**: Complete offline functionality with automatic sync
- ✅ **Conflict Resolution**: Handles concurrent modifications across devices  
- ✅ **Performance**: Optimized caching and lazy loading for large datasets
- ✅ **Real-time**: Live data updates when connected to backend

### **ViewModel Architecture (January 2025)**
- ✅ **Koin Injection**: Clean dependency injection with `koinInject { parametersOf() }`
- ✅ **MVI Pattern**: Consistent state management across all screens
- ✅ **Type Safety**: Eliminated ViewModelFactory complexity
- ✅ **Parameter Support**: Seamless ViewModel parameterization for detail screens

## **Development Guidelines**

### **Naming Conventions**
- **Package Structure**: `com.ampairs.{domain}.{layer}` (matches backend)
- **API Models**: Use `@SerialName` for snake_case backend compatibility
- **Database Entities**: Room entities with proper indexing and relationships
- **Navigation Routes**: Sealed classes with `@Serializable` for type safety

### **Backend Integration**
- **API Endpoints**: Follow backend REST patterns exactly (`/api/v1/{resource}`)
- **Error Handling**: Use backend `ApiResponse<T>` wrapper format
- **Authentication**: JWT tokens with automatic refresh and device tracking
- **Multi-tenancy**: Include workspace context in all API calls

### **Code Quality**
- **Offline-first**: Always design for offline operation with sync fallback
- **Error Recovery**: Graceful degradation when network/sync fails
- **Performance**: Lazy loading, pagination, and efficient caching
- **Testing**: Unit tests for ViewModels, integration tests for repositories

## **Common Issues & Solutions**

- **iOS Target**: Currently commented out - enable in `composeApp/build.gradle.kts` if needed
- **KSP Compatibility**: Version warnings may appear but don't affect functionality
- **Room Migration**: Check migration scripts when updating database schemas
- **Store5 Conflicts**: Use timestamp-based resolution for concurrent modifications
- **Network Timeouts**: Ktor client configured with proper timeout and retry policies

## **Integration with Backend**

This mobile app integrates seamlessly with the **Ampairs Spring Boot backend** by:

1. **Using identical domain models** and business logic patterns
2. **Following backend API contracts** exactly for REST endpoints  
3. **Implementing same authentication flows** with JWT and multi-device support
4. **Supporting multi-tenancy** with workspace-aware data isolation
5. **Maintaining feature parity** across web, mobile, and API clients

For backend development guidelines, refer to the main `/ampairs/CLAUDE.md` file.