# iOS Firebase Double Initialization Fix

**Date**: October 26, 2025
**Issue**: `NSException 'com.firebase.core' - Default app has already been configured`

## Problem

Firebase was being initialized **twice**, causing the app to crash on launch:

1. **AppDelegate.swift**: Called `FirebaseApp.configure()`
2. **KMPInitializer.kt**: Also called `FIRApp.configure()` unconditionally

Stack trace showed:
```
*** Terminating app due to uncaught exception 'com.firebase.core',
reason: 'Default app has already been configured.'
```

## Root Cause

The initialization flow was:
```
AppDelegate.didFinishLaunchingWithOptions()
    ↓
FirebaseApp.configure() ✅ (First time - OK)
    ↓
KMPInitializerKt.onDidFinishLaunchingWithOptions()
    ↓
FIRApp.configure() ❌ (Second time - CRASH)
```

Firebase SDK throws an exception if `configure()` is called more than once.

## Solution

### Fixed File: `composeApp/src/iosMain/kotlin/com/ampairs/auth/firebase/KMPInitializer.kt`

**Before** (CRASHED):
```kotlin
fun onDidFinishLaunchingWithOptions() {
    println("KMP Initializer: Starting setup...")
    FIRApp.configure() // ❌ Unconditional - causes crash
    println("KMP Initializer: Firebase Configured.")
}
```

**After** (FIXED):
```kotlin
/**
 * KMP Initializer called from AppDelegate
 *
 * NOTE: Firebase is now initialized in AppDelegate.swift BEFORE this function is called.
 * Do NOT call FIRApp.configure() here as it will cause a crash due to double initialization.
 */
fun onDidFinishLaunchingWithOptions() {
    println("KMP Initializer: Starting KMP-specific setup...")

    // Firebase is already configured in AppDelegate.swift
    // Add any other KMP initialization here if needed

    println("KMP Initializer: ✅ Setup complete")
}
```

## Verification - All Initialization Points Protected

### 1. AppDelegate.swift ✅
```swift
func application(_ application: UIApplication,
                 didFinishLaunchingWithOptions launchOptions: ...) -> Bool {
    if FirebaseApp.app() == nil {  // ✅ Check before configuring
        FirebaseApp.configure()
    }

    KMPInitializerKt.onDidFinishLaunchingWithOptions()
    return true
}
```

### 2. KMPInitializer.kt ✅
```kotlin
fun onDidFinishLaunchingWithOptions() {
    // ✅ No Firebase configuration - handled by AppDelegate
    println("KMP Initializer: ✅ Setup complete")
}
```

### 3. FirebaseAuthProvider.ios.kt ✅
```kotlin
actual suspend fun initialize(): FirebaseAuthResult<Unit> {
    if (FIRApp.defaultApp() == null) {  // ✅ Check before configuring
        FIRApp.configure()
    }
    return FirebaseAuthResult.Success(Unit)
}
```

## Expected Console Output

After the fix, you should see:
```
AppDelegate: Starting initialization
AppDelegate: ✅ Firebase configured
KMP Initializer: Starting KMP-specific setup...
KMP Initializer: ✅ Setup complete
AppDelegate: ✅ KMP initialized
FirebaseAuthProvider: Firebase already initialized by AppDelegate
```

## Testing

Build and run:
```bash
./gradlew clean
./gradlew :composeApp:compileKotlinIosSimulatorArm64

# Or in Xcode
open iosApp/iosApp.xcworkspace
# Product → Clean Build Folder
# Product → Run
```

### Verify:
- [ ] App launches without crash
- [ ] Console shows "Firebase already initialized by AppDelegate"
- [ ] No double initialization errors
- [ ] Phone auth flow works normally

## Key Takeaway

**Firebase initialization hierarchy**:
```
AppDelegate.swift (Swift)
    ↓
Initializes Firebase ONCE
    ↓
Calls KMP Initializer
    ↓
KMP code uses already-initialized Firebase
```

**Rule**: Only initialize Firebase in **one place** - the earliest entry point (AppDelegate).

## Related Files

- ✅ `iosApp/iosApp/AppDelegate.swift` - Primary Firebase initialization
- ✅ `composeApp/src/iosMain/kotlin/com/ampairs/auth/firebase/KMPInitializer.kt` - Removed duplicate init
- ✅ `composeApp/src/iosMain/kotlin/com/ampairs/auth/firebase/FirebaseAuthProvider.ios.kt` - Defensive check

---

**Status**: ✅ Fixed - Firebase initialized only once in AppDelegate
**Safe to rebuild and test**: Yes