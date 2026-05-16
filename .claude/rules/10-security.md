# Security & Secrets

- NEVER commit secrets, JWT keys, DB credentials, or API tokens to source control.
- Use environment variables for all sensitive config (`SPRING_PROFILES_ACTIVE`, `DB_PASSWORD`, etc.).
- `keys/` directory is redacted — do not add real keys there.
- Local dependencies (DB, Redis, etc.) via `docker-compose.yml` only.
- JWT tokens include `device_id` claims; refresh tokens are device-scoped.
- Multiple concurrent logins per user/device pair are supported.
