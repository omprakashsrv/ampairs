# Payment Provider Credentials Directory

This directory contains credential files for payment provider integrations.

## Files

### `ampairs-google-play.json` (Placeholder)
Google Play service account credentials for Android in-app purchases.

**Current Status**: Contains placeholder values (safe for development)

**To Use Real Credentials**:

#### Option 1: Replace this file
1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Select your project (or create one)
3. Enable "Google Play Android Developer API"
4. Create a service account with appropriate permissions
5. Download the JSON key file
6. Replace this file with your real credentials

#### Option 2: Use environment variable (Production)
```bash
export GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_PATH=/etc/ampairs/keys/google-play-service-account.json
```

#### Option 3: Use GitHub Secret (CI/CD)
See `.github/SECRETS.md` for instructions on configuring the `GOOGLE_PLAY_SERVICE_ACCOUNT_KEY` secret.

## Security Notes

⚠️ **IMPORTANT**:
- This directory is protected by `.gitignore` to prevent accidental commits of real credentials
- Only the placeholder file `ampairs-google-play.json` is tracked in git
- Never commit real service account credentials to version control
- In production, use environment variables or secure secret management (e.g., AWS Secrets Manager, HashiCorp Vault)

## File Format

The Google Play service account JSON should have this structure:
```json
{
  "type": "service_account",
  "project_id": "your-project-id",
  "private_key_id": "your-key-id",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "your-service-account@developer.gserviceaccount.com",
  "client_id": "your-client-id",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/..."
}
```

## Grant Permissions

After creating the service account, grant it access in Google Play Console:

1. Go to [Google Play Console](https://play.google.com/console)
2. Navigate to Setup > API access
3. Link the service account
4. Grant these permissions:
   - "View financial data"
   - "Manage orders and subscriptions"

## Other Payment Providers

For other payment providers (Apple, Razorpay, Stripe), credentials are configured via environment variables. See `.env.example` for details.

## Related Documentation

- `.github/SECRETS.md` - GitHub secrets configuration for CI/CD
- `.env.example` - Environment variables for all payment providers
- `subscription/MIGRATION_GUIDE.md` - Database migration instructions
