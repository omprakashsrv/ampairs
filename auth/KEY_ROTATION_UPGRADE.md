# RSA Key Manager Upgrade: Multi-Key Persistence

## Overview

The RSA Key Manager has been enhanced with **multi-key persistence** to support truly graceful key rotation that survives application restarts.

## What Changed

### Before (Single-Key Storage)
```
auth/keys/
├── private.pem          ← Overwritten on rotation
├── public.pem           ← Overwritten on rotation
└── metadata.json        ← Only tracked current key
```

**Problem**: Old keys were destroyed during rotation, causing old tokens to fail after restart.

### After (Multi-Key Versioned Storage)
```
auth/keys/
├── d6bf4b8e_2025-11-02/     ← Versioned by keyId + date
│   ├── private.pem
│   └── public.pem
├── aaaa1111_2025-10-01/     ← Old key preserved
│   ├── private.pem
│   └── public.pem
├── metadata.json            ← Tracks ALL keys
├── private.pem.legacy       ← Backup of old format
└── public.pem.legacy        ← Backup of old format
```

## New Features

### 1. **Versioned Key Storage**
- Each key stored in `keys/{keyId}_{date}/` directory
- Old keys never overwritten
- Full key history preserved

### 2. **Multi-Key metadata.json**
```json
{
    "currentKeyId": "d6bf4b8e",
    "keys": [
        {
            "keyId": "d6bf4b8e",
            "algorithm": "RS256",
            "createdAt": "2025-11-02T17:06:19.948775Z",
            "expiresAt": "2026-01-31T17:06:19.948770Z",
            "isActive": true,
            "isCurrent": true,
            "keyDirectory": "keys/d6bf4b8e_2025-11-02"
        },
        {
            "keyId": "aaaa1111",
            "algorithm": "RS256",
            "createdAt": "2025-10-01T12:00:00.000Z",
            "expiresAt": "2025-11-30T12:00:00.000Z",
            "isActive": false,
            "isCurrent": false,
            "keyDirectory": "keys/aaaa1111_2025-10-01"
        }
    ]
}
```

### 3. **Load All Keys on Startup**
- Loads current key + all historical keys
- Old tokens work after restart
- Keys validated and expired ones skipped

### 4. **Warning Logs for Old Keys**
```
⚠️  Using OLD key for token verification: aaaa1111 (age: 32 days, expires in: 28 days)
```

### 5. **Automatic Legacy Migration**
- Detects old single-key format
- Migrates to versioned storage
- Backs up old files as `.legacy`

### 6. **Enhanced Cleanup**
- Expired keys removed from memory AND disk
- metadata.json updated automatically
- Directory structure cleaned up

## Migration Path

### Automatic Migration

The system **automatically migrates** existing keys on startup:

1. Detects `keys/private.pem` and `keys/public.pem`
2. Loads key metadata from old format
3. Creates versioned directory: `keys/{keyId}_{date}/`
4. Stores keys in new format
5. Backs up old files: `*.pem.legacy`
6. Updates `metadata.json` with new format

**No manual intervention required!**

### After Migration

Old file structure:
```
auth/keys/
├── private.pem.legacy       ← Backup (safe to delete after verification)
├── public.pem.legacy        ← Backup (safe to delete after verification)
├── metadata.json.legacy     ← Backup (safe to delete after verification)
├── d6bf4b8e_2025-11-02/     ← New versioned key
│   ├── private.pem
│   └── public.pem
└── metadata.json            ← New format
```

## Behavior Changes

| Scenario | Before | After |
|----------|--------|-------|
| **Key rotation during runtime** | ✅ Old tokens work | ✅ Old tokens work |
| **App restart (no rotation)** | ✅ Tokens work | ✅ Tokens work |
| **App restart (after rotation)** | ❌ Old tokens FAIL | ✅ Old tokens work |
| **Multiple rotations** | ❌ Only latest works | ✅ All valid keys work |
| **Key expiration** | ❌ Lost on restart | ✅ Persisted and enforced |

## Log Messages

### Initialization
```
✅ RSA Key Manager initialized: current=d6bf4b8e, history=2, total=3
```

### Key Loading
```
✓ Loaded CURRENT key: d6bf4b8e (created: 2025-11-02T17:06:19Z)
✓ Loaded HISTORICAL key: aaaa1111 (expires: 2025-11-30T12:00:00Z)
```

### Key Usage
```
✓ Using CURRENT key for verification: d6bf4b8e
⚠️  Using OLD key for token verification: aaaa1111 (age: 32 days, expires in: 28 days)
❌ Unknown key ID requested: xyz12345 (not found in current or history)
```

### Key Rotation
```
🔄 Starting key rotation
Moved key d6bf4b8e to history (still valid for verification)
Stored RSA key pair: abc98765 in keys/abc98765_2025-12-02
Updated metadata.json with 3 total keys
✅ Key rotation completed. New key ID: abc98765
```

### Cleanup
```
🧹 Cleaning up 2 expired keys
Deleted expired key directory: keys/old123_2025-09-01
✅ Cleaned up 2 keys from memory, 2 from disk
```

### Migration
```
🔄 Detected legacy key format, migrating to versioned storage...
Stored RSA key pair: d6bf4b8e in keys/d6bf4b8e_2025-11-02
✅ Successfully migrated legacy key d6bf4b8e to versioned storage
```

## Testing

### 1. Verify Migration
```bash
# Before starting application
ls -la auth/keys/

# Start application (migration happens automatically)
./gradlew :ampairs_service:bootRun

# Check logs for migration message
# Check new directory structure
ls -la auth/keys/
```

### 2. Test Key Rotation
```kotlin
// Trigger manual rotation (if endpoint available)
POST /api/v1/auth/keys/rotate

// Check metadata
cat auth/keys/metadata.json

// Verify old tokens still work
```

### 3. Test Application Restart
```bash
# 1. Login and save token
# 2. Rotate keys
# 3. Restart application
# 4. Use old token - should still work
```

## Configuration

No configuration changes required. The system uses existing settings from `application.yml`:

```yaml
security:
  jwt:
    algorithm: RS256
    keyRotation:
      enabled: true
      rotationInterval: PT720H     # 30 days
      keyLifetime: PT1440H          # 60 days (keys valid for 60 days)
    keyStorage:
      metadataPath: keys/metadata.json
```

## Benefits

1. **Zero-Downtime Rotation**: Users never logged out
2. **Restart Safety**: Old tokens work after restart
3. **Audit Trail**: Full history of all keys
4. **Automatic Cleanup**: Expired keys removed automatically
5. **Observability**: Warning logs for old key usage
6. **Backward Compatible**: Automatic migration from old format

## Rollback

If issues occur, restore the backup:

```bash
# Stop application
# Restore old files
mv auth/keys/private.pem.legacy auth/keys/private.pem
mv auth/keys/public.pem.legacy auth/keys/public.pem
mv auth/keys/metadata.json.legacy auth/keys/metadata.json

# Remove versioned directories
rm -rf auth/keys/*/

# Restore backup code
mv auth/src/main/kotlin/com/ampairs/auth/service/RsaKeyManager.kt.backup \
   auth/src/main/kotlin/com/ampairs/auth/service/RsaKeyManager.kt

# Rebuild
./gradlew :auth:build
```

## Support

For issues or questions:
1. Check application logs for error messages
2. Verify `auth/keys/` directory structure
3. Check `auth/keys/metadata.json` contents
4. Review backup files in `auth/keys/*.legacy`

---

**Status**: ✅ Implemented and Tested
**Version**: 1.0.0
**Date**: 2025-11-03
