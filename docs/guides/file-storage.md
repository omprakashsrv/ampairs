# Storage Path Fix for Image Files

## Issue
Images were being saved without proper file extensions in their object storage paths, making them appear as files without names or extensions.

## Root Cause
The `getFileExtension()` method was returning "unknown" for files without extensions, and there was no fallback logic to determine the extension from the MIME type.

## Solution

### 1. Enhanced File Extension Detection
Updated `getFileExtension()` method to:
- First try to extract extension from filename
- If no extension found, detect from MIME type:
  - `image/jpeg` → `jpg`
  - `image/png` → `png`
  - `image/gif` → `gif`
  - `image/webp` → `webp`
  - `image/bmp` → `bmp`
  - `image/tiff` → `tiff`
- Default to `jpg` if neither filename nor MIME type provide extension

### 2. Updated Entity Creation
Modified `createCustomerImageEntity()` to pass both filename and content type to `getFileExtension()` for better detection.

### 3. Storage Path Generation
The `generateStoragePath()` method in `CustomerImage` already correctly includes the extension:
```kotlin
fun generateStoragePath(): String {
    return "${workspaceSlug}/customer/${customerUid}/${uid}.${fileExtension}"
}
```

## Result
Images now save with proper file extensions in object storage:
- Before: `workspace/customer/CUS123/IMG456` (no extension)
- After: `workspace/customer/CUS123/IMG456.jpg` (with extension)

This ensures files are properly recognized by file systems and can be opened correctly.