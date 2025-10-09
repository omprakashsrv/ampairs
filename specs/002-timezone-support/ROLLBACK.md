# Rollback Procedure: Timezone Migration

**Feature**: Multi-timezone support (LocalDateTime → Instant migration)
**Branch**: `002-timezone-support`
**Version**: 2.0.0

## Pre-Migration Backup

### Database Backup
```bash
# Create backup before migration
mysqldump -u root -p munsi_app > backup_before_timezone_migration_$(date +%Y%m%d_%H%M%S).sql

# Verify backup file exists and has content
ls -lh backup_before_timezone_migration_*.sql

# Test backup (dry run - doesn't actually restore)
mysql -u root -p munsi_app --execute="SELECT 'Backup test successful';"
```

### Git State Backup
```bash
# Current branch state
git branch -a > git_state_before_migration.txt
git log -5 --oneline >> git_state_before_migration.txt

# Create backup branch
git branch backup/001-you-are-retail
```

## Rollback Scenarios

### Scenario 1: Code Issues (Tests Failing)
**When**: During development, tests fail after implementation

**Action**:
```bash
# Revert specific commits
git log --oneline  # Find commit to revert to
git reset --hard <commit-hash>

# Or revert entire branch
git checkout 001-you-are-retail
git branch -D 002-timezone-support
```

**Impact**: No production impact, development only

---

### Scenario 2: Database Issues (After Migration Script)
**When**: After running database migration, data integrity issues detected

**Action**:
```bash
# Stop application
./gradlew :ampairs_service:stop

# Restore database from backup
mysql -u root -p munsi_app < backup_before_timezone_migration_YYYYMMDD_HHMMSS.sql

# Verify restoration
mysql -u root -p munsi_app -e "
SELECT COUNT(*) as workspace_count FROM workspace;
SELECT MIN(created_at), MAX(created_at) FROM workspace;
"

# Restart application on old code
git checkout 001-you-are-retail
./gradlew :ampairs_service:bootRun
```

**Impact**: Downtime during restore (estimated 5-15 minutes)

---

### Scenario 3: Production Issues (After Deployment)
**When**: After production deployment, timezone bugs affecting users

**Action**:
```bash
# 1. Immediate: Revert to previous version
git checkout 001-you-are-retail
./gradlew :ampairs_service:build
# Deploy previous version

# 2. Restore database if data corruption detected
# (Same as Scenario 2)

# 3. Rollback frontend
cd ampairs-web
git checkout 001-you-are-retail
npm run build
# Deploy previous frontend

# 4. Rollback mobile (if deployed)
# Re-release previous version from app stores
```

**Impact**: Full rollback, 15-30 minutes downtime

---

### Scenario 4: Performance Degradation
**When**: Performance metrics show >20% degradation

**Action**:
```bash
# Quick rollback to previous version
git checkout 001-you-are-retail

# Investigate performance issue
# Check logs: tail -f logs/application.log | grep "Instant\|Jackson"

# If needed, restore database (Scenario 2)
```

**Impact**: Temporary performance impact until rollback complete

---

## Rollback Triggers

**Immediate Rollback Required If**:
- ❌ Error rate increases >1%
- ❌ Data loss detected (row count mismatch)
- ❌ NULL timestamps introduced in database
- ❌ API clients report widespread timestamp parsing errors
- ❌ Performance degradation >20%
- ❌ Critical user-facing timezone bugs

**Monitor But Don't Rollback If**:
- ⚠️ Minor formatting differences in UI (expected)
- ⚠️ Some test failures (fix forward)
- ⚠️ Individual user reports timezone confusion (provide guidance)

---

## Verification After Rollback

### Database Verification
```sql
-- Check row counts match pre-migration
SELECT
    'workspace' as table_name,
    COUNT(*) as current_count
FROM workspace
UNION ALL
SELECT 'customer', COUNT(*) FROM customer
UNION ALL
SELECT 'product', COUNT(*) FROM product
UNION ALL
SELECT 'orders', COUNT(*) FROM orders
UNION ALL
SELECT 'invoice', COUNT(*) FROM invoice;

-- Verify no NULL timestamps
SELECT 'workspace' as table_name, COUNT(*) as null_count
FROM workspace
WHERE created_at IS NULL OR updated_at IS NULL
UNION ALL
SELECT 'customer', COUNT(*)
FROM customer
WHERE created_at IS NULL OR updated_at IS NULL;

-- Should return 0 null_count for all tables
```

### Application Verification
```bash
# Check application starts
curl http://localhost:8080/actuator/health

# Check API responses
curl -H "Authorization: Bearer $TOKEN" \
     -H "X-Workspace-ID: $WORKSPACE" \
     http://localhost:8080/workspace/v1

# Verify timestamps in response
# Should NOT have 'Z' suffix after rollback
```

### User Experience Verification
- Login to web app - should work
- Create new entity - should save successfully
- View existing entities - timestamps should display
- No console errors in browser

---

## Post-Rollback Actions

1. **Document Issue**: Create detailed incident report
   - What triggered rollback?
   - What data was affected?
   - What was the user impact?

2. **Root Cause Analysis**:
   - Identify what went wrong
   - Why weren't issues caught in testing?
   - What tests need to be added?

3. **Fix Forward Plan**:
   - Address root cause in development
   - Add missing tests
   - Re-plan migration with lessons learned

4. **Communication**:
   - Notify stakeholders of rollback
   - Explain what happened
   - Provide timeline for re-attempt (if applicable)

---

## Testing Rollback Procedure

**IMPORTANT**: Test this rollback procedure in staging environment before production deployment!

```bash
# In staging:
1. Deploy timezone migration
2. Create some test data
3. Trigger rollback
4. Verify all checks pass
5. Document any issues with rollback procedure
```

---

## Emergency Contacts

- **Database Admin**: [Contact info]
- **DevOps Lead**: [Contact info]
- **Backend Lead**: [Contact info]
- **Product Owner**: [Contact info]

---

## Backup Retention

- Keep database backup for **30 days** after successful migration
- Keep git backup branch for **90 days**
- Document backup locations in team wiki

---

*Rollback procedure created: 2025-01-09*
*Last updated: 2025-01-09*
