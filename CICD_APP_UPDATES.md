# CI/CD Pipeline for Desktop App Updates

Automated release pipeline for publishing desktop app updates to S3 and registering them in the database.

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [GitHub Actions Setup](#github-actions-setup)
- [Manual Publishing](#manual-publishing)
- [Jenkins Setup](#jenkins-setup)
- [GitLab CI Setup](#gitlab-ci-setup)
- [Troubleshooting](#troubleshooting)

---

## Overview

The CI/CD pipeline automates the following tasks:

1. **Build** desktop apps for macOS, Windows, and Linux
2. **Calculate** SHA-256 checksums for file integrity
3. **Upload** binaries to private S3 bucket
4. **Register** version metadata in database via API
5. **Create** GitHub releases with download links
6. **Notify** users through in-app update checker

### Security Features
- Private S3 bucket (no public URLs)
- Backend-controlled file streaming
- Rate limiting (1 download per 10 seconds)
- Checksum verification
- Admin-only API access

---

## Architecture

```
┌─────────────────┐
│  Git Tag Push   │
│   (v1.0.0.10)   │
└────────┬────────┘
         │
         v
┌─────────────────────────────────────┐
│         CI/CD Pipeline              │
│                                     │
│  1. Build App (macOS/Win/Linux)    │
│  2. Calculate Checksum (SHA-256)   │
│  3. Upload to S3 (private bucket)  │
│  4. Register in DB (POST /api)     │
│  5. Create GitHub Release          │
└────────┬────────────────────────────┘
         │
         v
┌─────────────────────────────────────┐
│    Infrastructure                   │
│                                     │
│  ┌──────────┐      ┌─────────────┐ │
│  │ S3 Bucket│◄─────┤   Backend   │ │
│  │ (Private)│      │  (Streams)  │ │
│  └──────────┘      └──────┬──────┘ │
│                           │        │
│                           v        │
│                    ┌──────────────┐│
│                    │  PostgreSQL  ││
│                    │  (app_       ││
│                    │   versions)  ││
│                    └──────────────┘│
└─────────────────────────────────────┘
         │
         v
┌─────────────────────────────────────┐
│      Desktop Clients                │
│                                     │
│  1. Check for updates (no auth)    │
│  2. Download via backend (streamed)│
│  3. Verify checksum                │
│  4. Install update                 │
└─────────────────────────────────────┘
```

---

## GitHub Actions Setup

### Prerequisites

1. **Create API Key** for GitHub Actions (see [API_KEY_AUTHENTICATION.md](API_KEY_AUTHENTICATION.md)):
   - Login as admin → Settings → API Keys
   - Create key with name "GitHub Actions - App Updates"
   - Scope: `APP_UPDATES`
   - Copy the key (shown only once!)

2. **GitHub Repository Secrets** (Settings → Secrets and variables → Actions):
   ```
   AWS_ACCESS_KEY_ID          - IAM user with S3 write access
   AWS_SECRET_ACCESS_KEY      - IAM user secret key
   AMPAIRS_API_KEY           - API Key for authentication (amp_xxx...)
   ```

3. **IAM Policy** for GitHub Actions:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": [
           "s3:PutObject",
           "s3:PutObjectAcl",
           "s3:GetObject"
         ],
         "Resource": "arn:aws:s3:::ampairs-app-updates/updates/*"
       }
     ]
   }
   ```

### Workflow File

The workflow is already set up at `.github/workflows/release-desktop-app.yml`.

### Triggering a Release

1. **Create an annotated tag** with release notes:
   ```bash
   git tag -a v1.0.0.10 -m "Release 1.0.0.10

   ## What's New
   - New feature: Dark mode support
   - Bug fix: Fixed crash on startup
   - Performance: Improved loading times

   ## Breaking Changes
   - Minimum supported version: 1.0.0.5"
   ```

2. **Push the tag**:
   ```bash
   git push origin v1.0.0.10
   ```

3. **Monitor the workflow**:
   - Go to Actions tab in GitHub
   - Watch the `Release Desktop App` workflow
   - All 3 platforms build in parallel

### Workflow Steps

For each platform (macOS, Windows, Linux):

1. ✅ Checkout code
2. ✅ Setup Java 21
3. ✅ Extract version from tag
4. ✅ Build app (`./gradlew packageDistribution`)
5. ✅ Find built artifact
6. ✅ Calculate SHA-256 checksum
7. ✅ Calculate file size
8. ✅ Upload to S3 (private)
9. ✅ Register in database via API
10. ✅ Create GitHub release

### Expected Output

After successful run:
- ✅ 3 binaries in S3: `s3://ampairs-app-updates/updates/macos-1.0.0.10.dmg` (etc.)
- ✅ 3 database entries with status `is_active=true`
- ✅ 1 GitHub release with 3 assets
- ✅ Users receive update notifications immediately

---

## Manual Publishing

### Using the Helper Script

For manual releases or other CI/CD systems, use the provided script:

```bash
./scripts/publish-app-update.sh <file> <version> <platform> [options]
```

### Examples

**Basic release:**
```bash
export AMPAIRS_ADMIN_TOKEN="your-jwt-token"
export AWS_ACCESS_KEY_ID="your-aws-key"
export AWS_SECRET_ACCESS_KEY="your-aws-secret"

./scripts/publish-app-update.sh \
  Ampairs-1.0.0.10.dmg \
  1.0.0.10 \
  MACOS
```

**Mandatory update with release notes:**
```bash
./scripts/publish-app-update.sh \
  Ampairs-1.0.0.11.dmg \
  1.0.0.11 \
  MACOS \
  --mandatory \
  --min-version 1.0.0.5 \
  --release-notes release-notes.md
```

**Dry run (validation only):**
```bash
./scripts/publish-app-update.sh \
  Ampairs.msi \
  1.0.0.12 \
  WINDOWS \
  --dry-run
```

### Script Features

- ✅ Validates all inputs before uploading
- ✅ Calculates SHA-256 checksum automatically
- ✅ Extracts version code from version string
- ✅ Supports markdown release notes
- ✅ Interactive confirmation (unless in CI)
- ✅ Colored output for easy monitoring
- ✅ Comprehensive error handling

---

## Jenkins Setup

### Jenkinsfile

```groovy
pipeline {
    agent any

    parameters {
        string(name: 'VERSION', description: 'Version (e.g., 1.0.0.10)')
        choice(name: 'PLATFORM', choices: ['MACOS', 'WINDOWS', 'LINUX'])
        booleanParam(name: 'IS_MANDATORY', defaultValue: false)
    }

    environment {
        AWS_REGION = 'ap-south-1'
        S3_BUCKET = 'ampairs-app-updates'
        API_BASE_URL = 'https://api.ampairs.in'
        AMPAIRS_ADMIN_TOKEN = credentials('ampairs-admin-token')
        AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
        AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
    }

    stages {
        stage('Build') {
            steps {
                sh './gradlew :ampairs-app:packageDistribution${PLATFORM}'
            }
        }

        stage('Publish') {
            steps {
                script {
                    def artifactPath = sh(
                        script: "find ampairs-app/build -name '*.dmg' -o -name '*.msi' -o -name '*.deb' | head -n 1",
                        returnStdout: true
                    ).trim()

                    def mandatoryFlag = params.IS_MANDATORY ? '--mandatory' : ''

                    sh """
                        ./scripts/publish-app-update.sh \\
                            ${artifactPath} \\
                            ${params.VERSION} \\
                            ${params.PLATFORM} \\
                            ${mandatoryFlag}
                    """
                }
            }
        }
    }

    post {
        success {
            echo 'Release published successfully! 🎉'
        }
        failure {
            echo 'Release failed! Check logs.'
        }
    }
}
```

### Jenkins Credentials Setup

1. Go to Jenkins → Manage Jenkins → Credentials
2. Add the following credentials:
   - `ampairs-admin-token` (Secret text)
   - `aws-access-key-id` (Secret text)
   - `aws-secret-access-key` (Secret text)

---

## GitLab CI Setup

### .gitlab-ci.yml

```yaml
stages:
  - build
  - publish

variables:
  AWS_REGION: ap-south-1
  S3_BUCKET: ampairs-app-updates
  API_BASE_URL: https://api.ampairs.in

# Build jobs for each platform
build:macos:
  stage: build
  tags: [macos]
  only: [tags]
  script:
    - ./gradlew :ampairs-app:packageDistributionMACOS
  artifacts:
    paths: [ampairs-app/build/compose/binaries/main/**/*.dmg]

build:windows:
  stage: build
  tags: [windows]
  only: [tags]
  script:
    - ./gradlew :ampairs-app:packageDistributionWINDOWS
  artifacts:
    paths: [ampairs-app/build/compose/binaries/main/**/*.msi]

build:linux:
  stage: build
  tags: [linux]
  only: [tags]
  script:
    - ./gradlew :ampairs-app:packageDistributionLINUX
  artifacts:
    paths: [ampairs-app/build/compose/binaries/main/**/*.deb]

# Publish jobs
publish:macos:
  stage: publish
  tags: [macos]
  only: [tags]
  dependencies: [build:macos]
  script:
    - VERSION=${CI_COMMIT_TAG#v}
    - ARTIFACT=$(find ampairs-app/build -name '*.dmg' | head -n 1)
    - |
      ./scripts/publish-app-update.sh \
        $ARTIFACT \
        $VERSION \
        MACOS

publish:windows:
  stage: publish
  tags: [windows]
  only: [tags]
  dependencies: [build:windows]
  script:
    - VERSION=${CI_COMMIT_TAG#v}
    - ARTIFACT=$(find ampairs-app/build -name '*.msi' | head -n 1)
    - |
      ./scripts/publish-app-update.sh \
        $ARTIFACT \
        $VERSION \
        WINDOWS

publish:linux:
  stage: publish
  tags: [linux]
  only: [tags]
  dependencies: [build:linux]
  script:
    - VERSION=${CI_COMMIT_TAG#v}
    - ARTIFACT=$(find ampairs-app/build -name '*.deb' | head -n 1)
    - |
      ./scripts/publish-app-update.sh \
        $ARTIFACT \
        $VERSION \
        LINUX
```

### GitLab CI/CD Variables

Settings → CI/CD → Variables:
- `AMPAIRS_ADMIN_TOKEN` (Masked, Protected)
- `AWS_ACCESS_KEY_ID` (Masked, Protected)
- `AWS_SECRET_ACCESS_KEY` (Masked, Protected)

---

## Version Numbering Scheme

### Format: `MAJOR.MINOR.PATCH.BUILD`

Example: `1.0.0.10`

- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes
- **BUILD**: Incremental build number (used as version_code)

### Git Tags

- Tag format: `v1.0.0.10`
- Must start with `v`
- Must have 4 numeric components

### Version Code

- Extracted automatically from version string
- Example: `1.0.0.10` → version_code = `10`
- Used for version comparison (simpler than semantic versioning)

---

## Testing the Pipeline

### 1. Test Script Locally

```bash
# Dry run
./scripts/publish-app-update.sh \
  test-app.dmg \
  1.0.0.999 \
  MACOS \
  --dry-run
```

### 2. Test S3 Upload

```bash
# Create test file
dd if=/dev/zero of=test.dmg bs=1M count=10

# Upload test
./scripts/publish-app-update.sh \
  test.dmg \
  1.0.0.999 \
  MACOS

# Verify in S3
aws s3 ls s3://ampairs-app-updates/updates/

# Clean up
aws s3 rm s3://ampairs-app-updates/updates/macos-1.0.0.999.dmg
```

### 3. Test Database Registration

```bash
# Check database
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  https://api.ampairs.in/api/v1/app-updates

# Should see version 1.0.0.999

# Clean up test version
curl -X DELETE \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  https://api.ampairs.in/api/v1/app-updates/{test-uid}
```

---

## Troubleshooting

### Build Fails

**Error:** `packageDistribution not found`
```bash
# Fix: Check Gradle task name
./gradlew tasks --all | grep package
```

**Error:** `Java version mismatch`
```bash
# Fix: Ensure Java 21
java -version
```

### Upload Fails

**Error:** `Access Denied (S3)`
```bash
# Fix: Check IAM policy
aws s3 ls s3://ampairs-app-updates/ --debug
```

**Error:** `Invalid credentials`
```bash
# Fix: Verify AWS credentials
aws sts get-caller-identity
```

### Registration Fails

**Error:** `401 Unauthorized`
```bash
# Fix: Check admin token
curl -H "Authorization: Bearer $TOKEN" \
  https://api.ampairs.in/api/v1/app-updates
```

**Error:** `Version already exists`
```bash
# Fix: Increment version number or delete existing version
# Check existing versions
curl -H "Authorization: Bearer $TOKEN" \
  https://api.ampairs.in/api/v1/app-updates \
  | jq '.data[] | select(.version == "1.0.0.10")'
```

### GitHub Actions Fails

**Error:** `Secret not found`
```bash
# Fix: Add secrets in GitHub repository settings
# Settings → Secrets and variables → Actions
```

**Error:** `Artifact not found`
```bash
# Fix: Check build output path
# Verify in workflow logs or adjust find command
```

---

## Monitoring and Alerts

### Slack Notifications (Optional)

Add to GitHub Actions workflow:

```yaml
- name: Notify Slack
  if: always()
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    text: 'Release ${{ steps.version.outputs.version }} - ${{ matrix.platform }}'
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

### Email Notifications (Jenkins)

Add to Jenkinsfile:

```groovy
post {
    success {
        mail to: 'team@ampairs.in',
             subject: "Release ${params.VERSION} Published",
             body: "Successfully published ${params.PLATFORM} version."
    }
    failure {
        mail to: 'team@ampairs.in',
             subject: "Release ${params.VERSION} FAILED",
             body: "Failed to publish ${params.PLATFORM} version. Check logs."
    }
}
```

---

## Best Practices

1. **Always use annotated tags** with release notes
2. **Test locally** before pushing tags
3. **Monitor S3 costs** (CloudWatch billing alerts)
4. **Keep secrets secure** (rotate regularly)
5. **Version incrementally** (never skip version codes)
6. **Document breaking changes** in release notes
7. **Test downloads** after each release
8. **Monitor error rates** in backend logs
9. **Set up backup S3 bucket** for disaster recovery
10. **Use semantic versioning** for clarity

---

## Security Checklist

- [ ] S3 bucket is private (no public-read ACL)
- [ ] AWS IAM user has minimal permissions
- [ ] Admin API tokens are rotated regularly
- [ ] Secrets are stored securely (GitHub Secrets, Vault)
- [ ] CI/CD logs don't expose secrets
- [ ] Rate limiting is enabled (1 req/10s)
- [ ] Checksums are verified on client side
- [ ] Downloads are streamed through backend
- [ ] Backend has S3 access logging enabled
- [ ] CloudTrail logs all S3 API calls

---

## Support

For issues or questions:
- Backend API: `core/src/main/kotlin/com/ampairs/core/appupdate/README.md`
- GitHub Actions: `.github/workflows/release-desktop-app.yml`
- Helper Script: `scripts/publish-app-update.sh`

---

**Generated with Claude Code**
