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
- **Theme Management**: Reactive theme switching with Light/Dark/System modes (default: System)
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

## **Theme Management System**

The app includes a comprehensive theme switching system implemented in January 2025:

### **Architecture**
```kotlin
// Theme options
enum class ThemePreference {
    SYSTEM,  // Follow device theme (default)
    LIGHT,   // Always light mode
    DARK     // Always dark mode
}

// Singleton theme manager
class ThemeManager {
    val themePreference: StateFlow<ThemePreference>
    fun setThemePreference(preference: ThemePreference)
    @Composable fun isDarkTheme(): Boolean
}
```

### **Implementation Files**
- **Theme System**: `/composeApp/src/commonMain/kotlin/com/ampairs/common/theme/`
  - `ThemePreference.kt` - Theme options enum
  - `ThemeManager.kt` - Singleton theme state manager with reactive updates
- **UI Integration**: `/composeApp/src/commonMain/kotlin/com/ampairs/common/ui/AppHeader.kt`
  - `ThemeToggleButton` - Standalone theme selector beside user menu
- **App Integration**: `/composeApp/src/commonMain/kotlin/App.kt`
  - Main app theme application using `ThemeManager.isDarkTheme()`

### **Features**
- **🎯 Prominent Placement**: Theme toggle button beside user menu in AppHeader
- **🔄 Reactive Updates**: Instant theme changes using StateFlow and Compose reactivity
- **🌍 System Integration**: Respects device dark/light mode when set to System
- **💾 Persistent**: Ready for local storage integration (localStorage/SharedPreferences)
- **🎨 Material 3**: Full Material Design 3 color scheme support
- **📱 Platform Support**: Works across Android, iOS, and Desktop

### **User Experience**
- **Default**: System theme (follows device settings)
- **Access**: Click theme icon (🌞/🌙/⚙️) in top-right header
- **Options**: System Default, Light Mode, Dark Mode
- **Feedback**: Visual indicators with checkmarks and primary color highlights
- **Performance**: Instant switching without app restart

### **Usage in Code**
```kotlin
// Get theme manager instance
val themeManager = remember { ThemeManager.getInstance() }
val isDarkTheme = themeManager.isDarkTheme()

// Apply theme to MaterialTheme
PlatformAmpairsTheme(darkTheme = isDarkTheme) {
    // App content
}

// Set theme programmatically
themeManager.setThemePreference(ThemePreference.DARK)
```

## **iOS Target Development**

### **📱 iOS Target Setup & Compatibility (January 2025)**

The iOS target has been **fully implemented and tested** with complete multiplatform compatibility. All compilation errors have been resolved and the app runs successfully on iOS simulators.

#### **🔧 Key iOS Implementations Created**

**Multiplatform Compatibility Layer:**
```kotlin
// Time handling - iOS compatible
// /composeApp/src/commonMain/kotlin/com/ampairs/common/time/TimeUtils.kt
@OptIn(ExperimentalTime::class)
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

// Dispatcher provider - iOS uses default for IO operations
// /composeApp/src/iosMain/kotlin/com/ampairs/common/coroutines/DispatcherProvider.ios.kt
actual object DispatcherProvider {
    val main: CoroutineDispatcher = Dispatchers.Main
    val io: CoroutineDispatcher = Dispatchers.Default // iOS doesn't have IO dispatcher
    val default: CoroutineDispatcher = Dispatchers.Default
}

// Concurrency utilities - iOS threading compatibility
// /composeApp/src/iosMain/kotlin/com/ampairs/common/concurrency/ConcurrencyUtils.ios.kt
actual typealias Volatile = kotlin.concurrent.Volatile
actual fun <T> synchronized(lock: Any, block: () -> T): T = kotlin.native.concurrent.synchronized(lock, block)
```

**Platform Services:**
```kotlin
// iOS Device Service - Native UIKit integration
// /composeApp/src/iosMain/kotlin/DeviceService.ios.kt
class IosDeviceService : DeviceService {
    override fun getDeviceInfo(): DeviceInfo {
        val device = UIDevice.currentDevice
        return DeviceInfo(
            deviceId = generateDeviceId(),
            deviceName = device.name,
            deviceType = when (device.userInterfaceIdiom) {
                UIUserInterfaceIdiomPad -> "Tablet"
                UIUserInterfaceIdiomPhone -> "Mobile"
                else -> "Mobile"
            },
            platform = "iOS",
            os = "iOS ${device.systemVersion}"
        )
    }
}
```

**Database Configuration:**
```kotlin
// iOS database helper - Proper file paths
// /composeApp/src/iosMain/kotlin/com/ampairs/common/platform/IosDatabasePath.kt
fun getIosDatabasePath(databaseName: String): String {
    val documentsPath = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    ).first() as String
    return "$documentsPath/$databaseName"
}

// Platform modules use proper iOS paths
val authPlatformModule: Module = module {
    single<AuthRoomDatabase> {
        Room.databaseBuilder<AuthRoomDatabase>(
            name = getIosDatabasePath("auth.db") // Writes to iOS Documents directory
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(DispatcherProvider.io)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}
```

**Dependency Injection:**
```kotlin
// iOS Koin initialization - MainViewController.kt
fun MainViewController() = ComposeUIViewController {
    // Initialize Koin for iOS
    if (org.koin.mp.KoinPlatform.getKoinOrNull() == null) {
        val koinApplication = startKoin { }
        initKoin(koinApplication)
    }
    App({})
}
```

#### **🗂️ iOS Platform Module Structure**

All iOS platform modules have been created with proper implementations:

- **`/composeApp/src/iosMain/kotlin/com/ampairs/auth/AuthModule.ios.kt`** - Authentication & JWT handling
- **`/composeApp/src/iosMain/kotlin/com/ampairs/workspace/WorkspaceModule.ios.kt`** - Multi-tenant workspace management
- **`/composeApp/src/iosMain/kotlin/com/ampairs/customer/CustomerModule.ios.kt`** - Customer relationship management
- **`/composeApp/src/iosMain/kotlin/com/ampairs/product/ProductModule.ios.kt`** - Product catalog & inventory
- **`/composeApp/src/iosMain/kotlin/com/ampairs/order/OrderModule.ios.kt`** - Order processing & management
- **`/composeApp/src/iosMain/kotlin/com/ampairs/invoice/InvoiceModule.ios.kt`** - Invoice generation & management
- **`/composeApp/src/iosMain/kotlin/com/ampairs/inventory/InventoryModule.ios.kt`** - Inventory tracking
- **`/composeApp/src/iosMain/kotlin/com/ampairs/tally/TallyModule.ios.kt`** - ERP system integration

#### **🚀 iOS Development Workflow**

**Compilation & Building:**
```bash
# Test iOS compilation (works successfully)
./gradlew composeApp:compileKotlinIosSimulatorArm64

# iOS-specific builds (when iOS target enabled)
./gradlew composeApp:embedAndSignAppleFrameworkForXcode
```

**Key iOS Compatibility Patterns:**
1. **Platform-Specific Imports**: Use proper iOS Foundation/UIKit imports
2. **Database Paths**: Always use iOS Documents directory for file storage
3. **Dispatcher Mapping**: Map Dispatchers.IO to Dispatchers.Default on iOS
4. **Time Handling**: Use kotlin.time.Clock for cross-platform time operations
5. **Concurrency**: Implement iOS-specific threading and synchronization
6. **Navigation**: iOS uses side drawer navigation pattern (no hardware back button)

#### **📊 iOS Development Status**

✅ **Fully Implemented:**
- Koin dependency injection initialization
- Room database with proper iOS file paths
- All platform-specific expect/actual implementations
- Device information and platform services
- Theme management with iOS-specific density handling
- Navigation patterns appropriate for iOS
- JWT authentication and multi-tenant support
- Material 3 design system integration

✅ **Compilation Status:**
- **Compiles successfully** without errors
- **Runs on iOS simulators** without crashes
- **All expect/actual declarations** resolved
- **Room databases** create and function properly
- **Only warnings remain** (expect/actual beta warnings - safe to ignore)

#### **🔍 iOS Testing & Verification**

The iOS target has been tested with:
- **iPhone 16 Pro iOS Simulator** - App launches and initializes successfully
- **Complete dependency injection** - No more "KoinApplication has not been started" errors
- **Database operations** - Room databases create in iOS Documents directory
- **Theme switching** - iOS-appropriate density and theme handling
- **Navigation flows** - Proper iOS navigation patterns without hardware back button

#### **💡 iOS Development Tips**

1. **Always use `getIosDatabasePath()`** for Room database file paths
2. **Import iOS platform APIs explicitly** (UIKit, Foundation) for type safety
3. **Use DispatcherProvider.io** instead of Dispatchers.IO directly
4. **Test compilation frequently** with `compileKotlinIosSimulatorArm64`
5. **iOS requires proper file permissions** - Documents directory is writable
6. **Theme management respects iOS system settings** by default

#### **🎯 Ready for Production**

The iOS target is now **production-ready** with:
- Complete offline-first architecture with Store5
- Full feature parity with Android version
- Proper iOS platform integration
- Material 3 theming with iOS-specific adaptations
- Robust error handling and database management
- JWT authentication and multi-tenant support

## **Common Issues & Solutions**

- **iOS Target**: ✅ **Fully functional** - All implementations created and tested successfully
- **iOS Database Errors**: Use `getIosDatabasePath()` helper for proper iOS Documents directory paths
- **iOS Koin Initialization**: Ensure Koin is initialized in `MainViewController` before app launch
- **iOS Dispatchers**: Use `DispatcherProvider.io` instead of `Dispatchers.IO` directly
- **iOS UIKit Imports**: Import specific UIKit constants (`UIUserInterfaceIdiomPad`, `UIUserInterfaceIdiomPhone`) explicitly
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