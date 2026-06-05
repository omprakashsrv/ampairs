# Ampairs Production Setup — Session Log

**Date:** 2026-05-25  
**Server:** ip-10-0-0-12  
**Service:** ampairs-production (Spring Boot / Java)

---

## 1. Service Discovery

Confirmed the following systemd units are installed:

| Unit | Status |
|------|--------|
| `ampairs-production.service` | enabled, active |
| `ampairs-db-backup.service` | disabled, failed |
| `ampairs-db-backup.timer` | enabled, waiting |

Service file location: `/etc/systemd/system/ampairs-production.service`  
Application jar: `/opt/ampairs/production/ampairs-service.jar`  
Runs as user: `ampairs`

---

## 2. Environment Variables — Secrets File

Created a secure environment file to hold database credentials instead of hardcoding them in the service unit file.

**File:** `/etc/ampairs/secrets.env`  
**Permissions:** `600` (owner: `ampairs:ampairs`)

```
DB_URL=jdbc:postgresql://10.0.0.13:5432/ampairs_prod
DB_USERNAME=postgres
DB_PASSWORD=<redacted>
```

These map to the Spring Boot `application.yml` placeholders:
```yaml
datasource:
  url: ${DB_URL:jdbc:postgresql://10.0.0.12:5432/ampairs_prod}
  username: ${DB_USERNAME:postgres}
  password: ${DB_PASSWORD:...}
```

---

## 3. Service File Update

Updated `/etc/systemd/system/ampairs-production.service` to reference the secrets file.

**Replaced** the commented-out DB block:
```ini
# Database Configuration (override in /etc/environment or use secrets)
# Environment="SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ampairs_prod"
# Environment="SPRING_DATASOURCE_USERNAME=ampairs_user"
# Environment="SPRING_DATASOURCE_PASSWORD=your_secure_password_here"
```

**With:**
```ini
# Database Configuration
EnvironmentFile=/etc/ampairs/secrets.env
```

Then reloaded systemd:
```bash
sudo systemctl daemon-reload
sudo systemctl restart ampairs-production
```

---

## 4. Database Investigation

### First failure — DB dialect error
```
Unable to determine Dialect without JDBC metadata
```
**Cause:** Could not connect to the database at all.

### Host check
- `10.0.0.13:5432` — reachable ✅
- `10.0.0.12:5432` — connection refused ❌

The default DB host in the jar (`10.0.0.12`) is different from the actual DB server (`10.0.0.13`). The `DB_URL` env var correctly points to `10.0.0.13`.

### Database missing
```bash
PGPASSWORD='...' psql -h 10.0.0.13 -U postgres -c "\l"
```
Result: `ampairs_prod` database did not exist — only `postgres`, `template0`, `template1`.

### Created the database
```bash
PGPASSWORD='...' psql -h 10.0.0.13 -U postgres -c "CREATE DATABASE ampairs_prod;"
```

---

## 5. Second Failure — Missing Schema

After creating the database, service still failed:

```
Schema validation: missing table [api_keys]
```

**Cause:** Hibernate is configured with `ddl-auto: validate`, meaning it expects all tables to already exist. The database is empty.

**Flyway investigation:** The jar contains Flyway libraries but **no SQL migration scripts** — only markdown documentation files. Flyway is effectively a no-op and will not create the schema.

```
BOOT-INF/classes/db/migration/MIGRATION_BASELINE.md
BOOT-INF/classes/db/migration/NO_MIGRATION_NEEDED.md
BOOT-INF/classes/db/migration/README.md
```

---

## 6. Current State & Next Step

The service is currently **failing to start** because the `ampairs_prod` database exists but has no tables.

**Pending decision — choose one:**

### Option A: Let Hibernate create the schema (fresh start)
Add to `/etc/ampairs/secrets.env`:
```
JPA_DDL_AUTO=create
```
Restart the service. Hibernate will auto-create all tables. After first successful start, change back to `validate`.

### Option B: Restore from a database dump
```bash
PGPASSWORD='...' psql -h 10.0.0.13 -U postgres -d ampairs_prod < /path/to/dump.sql
```
Then restart the service. Use this if you have an existing backup from another environment.

---

## Key File Locations

| File | Purpose |
|------|---------|
| `/etc/systemd/system/ampairs-production.service` | Systemd service unit |
| `/etc/ampairs/secrets.env` | DB credentials (mode 600) |
| `/opt/ampairs/production/ampairs-service.jar` | Application jar |
| `/opt/ampairs/production/keys/` | Firebase SDK key directory |
| `/var/log/ampairs/production.log` | Application log file |

## Useful Commands

```bash
# View live logs
sudo journalctl -u ampairs-production -f

# Restart service
sudo systemctl restart ampairs-production

# Check service status
sudo systemctl status ampairs-production

# Edit secrets
sudo nano /etc/ampairs/secrets.env

# After editing service file
sudo systemctl daemon-reload && sudo systemctl restart ampairs-production

# Connect to DB directly
PGPASSWORD='...' psql -h 10.0.0.13 -U postgres -d ampairs_prod
```
