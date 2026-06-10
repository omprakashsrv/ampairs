# 0004 — Secure-by-default OTP bypass and reCAPTCHA enforcement

- Status: Accepted
- Date: 2026-06-10

## Context

Auth has a development convenience: a hardcoded OTP that logs in any phone number when
`development-mode` + `allow-hardcoded` are on. The defaults shipped *open* — `OTP_DEV_MODE:true`,
`OTP_ALLOW_HARDCODED:true`, `OTP_HARDCODED_VALUE:123456` — in the base **and** production profiles.
The only thing preventing universal account takeover in production was `development-mode` not being
overridden. Separately, the reCAPTCHA check on `POST /auth/v1/verify/firebase` was commented out, and
payment providers booted with `PLACEHOLDER_*` secrets, failing silently at charge time instead of
at startup.

## Decision

- **OTP bypass is secure by default.** Production profile defaults are now
  `OTP_DEV_MODE:false`, `OTP_ALLOW_HARDCODED:false`, `OTP_HARDCODED_VALUE:` (empty). Running the
  bypass under the `production` profile additionally requires an explicit
  `OTP_ALLOW_HARDCODED_IN_PRODUCTION=true`, enforced by `OtpSecurityStartupCheck`, which fails fast
  on boot otherwise. This keeps the legitimate use (an app-store-review account) possible but
  deliberate and auditable.
- **reCAPTCHA on the Firebase endpoint is configurable, not commented out.**
  `RecaptchaConfiguration.enforceOnFirebase` (env `RECAPTCHA_ENFORCE_FIREBASE`) gates it, defaulting
  off because current mobile clients send no token on that flow and Firebase Phone Auth already
  attests the device; the endpoint is also covered by the strict `verify` rate-limit bucket. Flip it
  on once clients send a token.
- **Payments fail fast on placeholders.** `PaymentProviderConfiguration` only registers a provider
  whose credentials are actually configured; an unconfigured provider then fails with
  "Provider not configured" at call time and is logged at `error` under the production profile.
- Also fixed: `/auth/v1/refresh-token` (hyphen) never matched the rate-limit interceptor's
  `refresh_token` (underscore) key, silently demoting it to the strict verify limits.

## Consequences

- **Positive:** production cannot accidentally accept a hardcoded OTP; the auth endpoints' bot and
  rate-limit protections are explicit and on; payment misconfiguration surfaces loudly.
- **Negative:** a developer relying on the old open defaults must now set the dev env vars
  explicitly. Enabling `RECAPTCHA_ENFORCE_FIREBASE` before clients send a token will break Firebase
  login — sequence the client change first.
