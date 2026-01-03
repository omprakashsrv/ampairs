# GitHub Secrets Configuration

This document lists all GitHub secrets required for CI/CD deployment.

## Required Secrets

Configure these secrets in: **Repository Settings > Secrets and variables > Actions > Repository secrets**

### 1. SSH_PRIVATE_KEY
**Purpose**: SSH access to production server for deployment

**How to get**:
```bash
# Generate SSH key pair (if not already done)
ssh-keygen -t rsa -b 4096 -C "github-actions@ampairs.com" -f ~/.ssh/ampairs_deploy

# Copy the PRIVATE key content
cat ~/.ssh/ampairs_deploy
```

**Value format**: Raw private key content
```
-----BEGIN OPENSSH PRIVATE KEY-----
...
-----END OPENSSH PRIVATE KEY-----
```

**Add public key to server**:
```bash
# Copy public key to production server
cat ~/.ssh/ampairs_deploy.pub
# Add to ~/.ssh/authorized_keys on the server
```

---

### 2. FIREBASE_SERVICE_ACCOUNT_KEY
**Purpose**: Firebase Admin SDK for notifications and authentication

**How to get**:
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Go to Project Settings > Service Accounts
4. Click "Generate New Private Key"
5. Download the JSON file

**Value format**: Raw JSON content (do NOT add extra quotes)
```json
{
  "type": "service_account",
  "project_id": "your-project",
  "private_key_id": "...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "...",
  ...
}
```

**Important**:
- Copy the ENTIRE JSON content
- Do NOT wrap in additional quotes
- Preserve newline characters as `\n` in the JSON

---

### 3. GOOGLE_PLAY_SERVICE_ACCOUNT_KEY ⚠️ (Optional but Recommended)
**Purpose**: Google Play Billing verification for Android in-app purchases

**Status**: Optional - deployment will succeed with a warning if not set, but Google Play payment features will not work.

**How to get**:
1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Select your project (or create one)
3. Enable "Google Play Android Developer API"
4. Go to IAM & Admin > Service Accounts
5. Create service account with appropriate permissions
6. Create key (JSON format)
7. Download the JSON file

**Value format**: Raw JSON content (do NOT add extra quotes)
```json
{
  "type": "service_account",
  "project_id": "your-project-id",
  "private_key_id": "...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "...@developer.gserviceaccount.com",
  "client_id": "...",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  ...
}
```

**Grant permissions**:
1. Go to [Google Play Console](https://play.google.com/console)
2. Setup > API access
3. Link the service account
4. Grant "View financial data" and "Manage orders and subscriptions" permissions

**Important**:
- Copy the ENTIRE JSON content
- Do NOT wrap in additional quotes
- If not set, deployment continues but Android payments won't work

---

## Other Payment Provider Secrets (Environment Variables)

These are configured as environment variables on the production server, not as GitHub secrets:

### Apple App Store
```bash
export APPLE_SHARED_SECRET="your_app_store_shared_secret"
export APPLE_PRODUCTION=true
```

### Razorpay
```bash
export RAZORPAY_KEY_ID="rzp_live_xxxxxxxxxxxxx"
export RAZORPAY_KEY_SECRET="your_razorpay_key_secret"
export RAZORPAY_WEBHOOK_SECRET="your_razorpay_webhook_secret"
```

### Stripe
```bash
export STRIPE_SECRET_KEY="sk_live_xxxxxxxxxxxxx"
export STRIPE_WEBHOOK_SECRET="whsec_xxxxxxxxxxxxx"
```

---

## Verification

After adding secrets, verify in the GitHub Actions workflow output:

✅ Expected success messages:
```
✅ Firebase service account key prepared successfully
Key file size: 2361 bytes

✅ Google Play service account key prepared successfully
Key file size: 2405 bytes
```

⚠️ Warning if Google Play key not set:
```
⚠️  WARNING: GOOGLE_PLAY_SERVICE_ACCOUNT_KEY secret is not set
Google Play payment features will not work in production
Continuing deployment without Google Play credentials...
```

❌ Error messages indicate invalid secret format:
```
❌ ERROR: Firebase key is not valid JSON
```

---

## Security Best Practices

1. **Never commit secrets to repository**
   - Use `.gitignore` for local credential files
   - Use GitHub Secrets for CI/CD

2. **Rotate secrets regularly**
   - Generate new SSH keys annually
   - Rotate service account keys every 90 days

3. **Use separate credentials for environments**
   - Test/sandbox keys for development
   - Production keys only in GitHub Secrets

4. **Limit permissions**
   - Service accounts should have minimal required permissions
   - Use separate service accounts per service (Firebase, Google Play, etc.)

5. **Audit access**
   - Review who has access to GitHub Secrets
   - Monitor service account usage in cloud consoles

---

## Troubleshooting

### "Failed to create key file"
- Check that secret name matches exactly (case-sensitive)
- Verify secret is not empty

### "Key is not valid JSON"
- Remove any extra quotes around the JSON
- Ensure newlines are `\n` not actual line breaks
- Copy raw JSON directly from downloaded file

### "Permission denied" during deployment
- Verify SSH_PRIVATE_KEY matches public key on server
- Check server's `~/.ssh/authorized_keys` file

### Payment features not working in production
- Verify all payment provider secrets are set
- Check application logs for initialization errors
- Confirm service account permissions in provider consoles
