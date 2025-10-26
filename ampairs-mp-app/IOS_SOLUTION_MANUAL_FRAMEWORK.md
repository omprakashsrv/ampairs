# iOS Solution: Manual Framework Integration with Firebase CocoaPods

**Date**: January 2025
**Status**: ✅ Ready for Manual Setup

## Problem Summary

The core issue: **Cannot use both CocoaPods AND embedAndSign tasks together**.

## Solution Applied

**Hybrid Approach**:
- ✅ Use CocoaPods ONLY for Firebase (FirebaseAuth, FirebaseCore)
- ✅ Use Manual Framework Integration for ComposeApp (Kotlin framework)

### What Changed

**Podfile** - Now only Firebase:
```ruby
target 'iosApp' do
  # Firebase pods only
  pod 'FirebaseAuth'
  pod 'FirebaseCore'
end
```

**Removed**:
- `pod 'composeApp'` from Podfile
- "Compile Kotlin Framework" build phase from Xcode

## 🛠️ Manual Setup Required in Xcode

You need to add the Kotlin framework build phase manually in Xcode:

### Step 1: Open Xcode

```bash
cd /Users/omprakashsrv/IdeaProjects/ampairs/ampairs-mp-app
open iosApp/iosApp.xcworkspace
```

### Step 2: Add Build Phase

1. **Select Project** → `iosApp` in left sidebar
2. **Select Target** → `iosApp`
3. **Go to Build Phases** tab
4. **Click `+`** → **New Run Script Phase**
5. **Name it**: "Compile Kotlin Framework"
6. **Drag it** BEFORE "Compile Sources" (important!)
7. **Paste this script**:

```bash
if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
  exit 0
fi
cd "$SRCROOT/.."
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

### Step 3: Add Framework to Project

1. **In Project Navigator** → Select `iosApp` project
2. **Select Target** → `iosApp`
3. **Go to General** tab
4. **Scroll to "Frameworks, Libraries, and Embedded Content"**
5. **Click `+`**
6. **Click "Add Other"** → **Add Files...**
7. **Navigate to**: `composeApp/build/xcode-frameworks/debug/iphonesimulator/ComposeApp.framework`
8. **Select it** and click **Add**
9. **Set embed mode**: "Embed & Sign"

### Step 4: Update Framework Search Paths

1. **Select Target** → `iosApp`
2. **Go to Build Settings** tab
3. **Search for** `FRAMEWORK_SEARCH_PATHS`
4. **Add** (if not already there):
   ```
   $(inherited)
   $(PROJECT_DIR)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)
   ```

### Step 5: Clean and Build

1. **Clean Build Folder**: Product → Clean Build Folder (Cmd+Shift+K)
2. **Build**: Product → Build (Cmd+B)

## Expected Build Flow

```
1. [CP] Check Pods Manifest.lock (Firebase)
   ✓ Verifies Firebase pods

2. Compile Kotlin Framework
   → Runs: ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
   → Builds framework at: composeApp/build/xcode-frameworks/debug/iphonesimulator/
   → Contains: ComposeApp.framework with MainViewController

3. Compile Sources (Swift)
   → import ComposeApp ✓
   → MainViewController() available ✓

4. Link Binary with Libraries
   → Links ComposeApp.framework
   → Links Firebase frameworks

5. [CP] Embed Pods Frameworks (Firebase)
   → Embeds Firebase frameworks

6. Build Succeeded! 🎉
```

## Why This Works

| Component | Method | Reason |
|-----------|--------|--------|
| Firebase | CocoaPods | Firebase requires CocoaPods for proper SDK integration |
| ComposeApp | Manual (embed AndSign) | No CocoaPods conflict, full Kotlin/Native support |

## Alternative: Command Line Build

If you prefer command line:

```bash
# Build Kotlin framework first
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode

# Then build with xcodebuild
xcodebuild -workspace iosApp/iosApp.xcworkspace \
    -scheme iosApp \
    -configuration Debug \
    -sdk iphonesimulator \
    -destination 'platform=iOS Simulator,name=iPhone 15 Pro'
```

## Troubleshooting

### Error: "No such module 'ComposeApp'"

**Cause**: Framework not built or not found

**Solution**:
```bash
# Build framework
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode

# Check it exists
ls composeApp/build/xcode-frameworks/debug/iphonesimulator/ComposeApp.framework

# Clean Xcode build
# In Xcode: Cmd+Shift+K
```

### Error: "Cannot find 'MainViewController'"

**Cause**: Framework built but headers not found

**Solution**:
1. Verify framework is in Framework Search Paths
2. Verify framework is in "Frameworks, Libraries, and Embedded Content"
3. Clean build folder and rebuild

### Error: Still getting CocoaPods conflict

**Cause**: Old build phase still referencing embedAndSignPodAppleFrameworkForXcode

**Solution**:
1. Check build phases in Xcode
2. Ensure script uses `embedAndSignAppleFrameworkForXcode` (NOT embedAndSignPod*)
3. Ensure Podfile doesn't include composeApp pod

## Configuration Summary

| Item | Configuration |
|------|---------------|
| Podfile | Firebase only (no composeApp pod) |
| Build Phase | embedAndSignAppleFrameworkForXcode task |
| Framework Location | composeApp/build/xcode-frameworks/ |
| Framework Search Path | $(PROJECT_DIR)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME) |
| Embed Mode | Embed & Sign |

## After Successful Build

1. ⏳ **Test app on simulator** - Run (Cmd+R)
2. ⏳ **Add GoogleService-Info.plist** - For Firebase config
3. ⏳ **Test Firebase auth** - Verify authentication works

---

**Last Updated**: January 2025
**Status**: ✅ Pods updated, ready for manual Xcode setup
