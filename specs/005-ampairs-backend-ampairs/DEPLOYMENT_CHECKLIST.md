# Migration Deployment Checklist

## Pre-deployment (Staging + Production)
- [ ] Review latest migrations (`V4_*`) and ensure peer review completed.
- [ ] Confirm backups are scheduled (database snapshot or `mysqldump`).
- [ ] Verify `MIGRATION_BASELINE.md` is up to date for the target environment.
- [ ] Ensure application build passes (`./gradlew ciBuild`) and Flyway tests succeed locally.
- [ ] Communicate maintenance window to stakeholders (for production).

## Staging Deployment Verification
1. Backup staging database.
2. Apply migrations: `./gradlew :ampairs_service:flywayMigrate -Dspring.datasource.url=<staging-url>`.
3. Run smoke tests / health checks.
4. Inspect `flyway_schema_history` to confirm new versions installed.
5. Start application with `SPRING_PROFILES_ACTIVE=test` and watch for `ddl-auto:validate` success.
6. Sign off staging deployment before promoting to production.

## Production Deployment Verification
1. Enable maintenance mode (if supported) and notify users.
2. Backup production database.
3. Apply migrations with a dedicated user that has DDL permissions.
4. Monitor Flyway output for failures; abort and restore backup if errors occur.
5. Start application with `SPRING_PROFILES_ACTIVE=prod` and verify startup logs.
6. Run API smoke tests (authentication, unit listing, customer search).
7. Disable maintenance mode and announce completion.

## Monitoring & Alerting
- Watch application logs for `FlywayException`, `SchemaManagementException`, or `ddl-auto` validation failures.
- Query `flyway_schema_history` post-deployment to ensure all migrations are marked `success = 1`.
- Dashboard idea: track number of tables / indexes vs. expected baseline to detect drift.
- Configure alerting for repeated startup failures so the on-call engineer can respond quickly.

## Rollback Runbook
- **If migration fails before completion**: restore backup, run `flywayRepair` to clear failed entry, fix migration, reapply.
- **If failure occurs after deployment**: consider hotfix migration (higher version) instead of editing existing scripts.
- Drop newly created tables in reverse dependency order only after confirming data recovery plan.
- Document every manual SQL executed during rollback for future audits.

## Post-deployment
- [ ] Update release notes with migration versions applied.
- [ ] Close related tickets (`AMP-*`) in the tracker.
- [ ] Schedule retrospective if issues occurred.
- [ ] Archive logs and backup references in the deployment record.
