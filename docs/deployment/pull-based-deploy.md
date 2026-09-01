# Pull-based deployment

This is the secure successor to the SSH-push pipeline in `.github/workflows/deploy.yml`.
Instead of GitHub Actions opening an SSH session into production (which requires an
internet-facing port 22 and a root-capable deploy key stored in CI), the **server reaches
out** to fetch new builds and deploys itself. CI only **observes** the result.

```
  push to main ──▶ CI: build jar, upload to S3 + write latest.json   (publish-artifact.yml)
                                     │
                                     ▼
  server timer ──▶ poll latest.json ─▶ new commit? ─▶ download + verify sha256
                                     │                        │
                                     │                        ▼
                                     │             sudo ampairs-apply-jar (swap + restart + health gate, rollback on fail)
                                     ▼
  CI (optional) ─▶ poll /api/actuator/info until build.commit == github.sha   (verify-deployment.yml)
```

## Why this is safer

| | SSH push (old) | Pull (this) |
|---|---|---|
| Inbound SSH to prod | Required, open to GitHub's broad IP ranges | **Not required — close port 22** |
| Credential in CI | Long-lived, root-capable SSH key | S3 **write** key scoped to one prefix |
| Blast radius if CI secret leaks | Full root on the host | Can publish artifacts (mitigated by checksum + IAM scope) |
| Trust boundary | The SSH key | The **artifact store** (S3) — so lock its write access down |

The trust boundary moves to S3. That is the one thing you must get right: **only CI may write
to the artifact prefix, and the agent verifies the sha256 before executing anything.**

## What's in the repo

| File | Role |
|---|---|
| `.github/workflows/publish-artifact.yml` | CI: build + upload jar + `latest.json` to S3 |
| `.github/workflows/verify-deployment.yml` | CI: poll `/api/actuator/info`, confirm live commit + health |
| `scripts/ampairs-pull-deploy.sh` | Agent (unprivileged): poll, download, checksum-verify |
| `scripts/ampairs-apply-jar.sh` | Root helper: swap jar, restart, health gate, rollback |
| `scripts/ampairs-pull-deploy.{service,timer}` | systemd timer that runs the agent |
| `scripts/ampairs-deploy-sudoers` | The single NOPASSWD rule the agent needs |

The deployed version is published by `bootBuildInfo` (see `ampairs_service/build.gradle.kts`)
and surfaced at `GET /api/actuator/info` as `build.commit` / `build.version`.

## One-time server setup

> Do these on the host. Review each file first — nothing here is applied automatically.

### 1. Dedicated unprivileged user + AWS read profile
```bash
sudo useradd -r -s /usr/sbin/nologin -d /home/ampairs-deploy -m ampairs-deploy
# Configure an AWS profile named `deploy` for that user with READ-ONLY access to the
# artifact prefix (s3:GetObject on deploy/*). Use an instance role if on EC2.
```

### 2. Install scripts + helper (root-owned)
```bash
sudo install -m 0755 scripts/ampairs-pull-deploy.sh  /usr/local/bin/ampairs-pull-deploy.sh
sudo install -m 0755 scripts/ampairs-apply-jar.sh    /usr/local/sbin/ampairs-apply-jar
sudo install -m 0440 -o root -g root scripts/ampairs-deploy-sudoers /etc/sudoers.d/ampairs-deploy
sudo visudo -cf /etc/sudoers.d/ampairs-deploy      # MUST validate clean
```

### 3. Config
```bash
sudo mkdir -p /etc/ampairs
sudo tee /etc/ampairs/pull-deploy.env >/dev/null <<'ENV'
ARTIFACT_S3_BUCKET=your-bucket
ARTIFACT_S3_PREFIX=deploy
AWS_PROFILE=deploy
ENV
sudo chmod 640 /etc/ampairs/pull-deploy.env
sudo chown root:ampairs-deploy /etc/ampairs/pull-deploy.env
sudo mkdir -p /var/log/ampairs && sudo chown ampairs-deploy /var/log/ampairs
```

### 4. Enable the timer
```bash
sudo install -m 0644 scripts/ampairs-pull-deploy.service /etc/systemd/system/
sudo install -m 0644 scripts/ampairs-pull-deploy.timer   /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now ampairs-pull-deploy.timer
# Dry run the agent once:
sudo -u ampairs-deploy /usr/local/bin/ampairs-pull-deploy.sh
```

## CI configuration

Add under **Settings → Secrets and variables → Actions**:

- `vars.ARTIFACT_S3_BUCKET`, `vars.ARTIFACT_S3_PREFIX` (e.g. `deploy`), `vars.API_DOMAIN`
- `secrets.AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` — an IAM user whose policy is
  **`s3:PutObject` on `arn:aws:s3:::<bucket>/deploy/*` only**. This is the leak-blast-radius
  control: a stolen CI key can publish artifacts but cannot touch anything else, and the
  agent still rejects any jar whose sha256 doesn't match the manifest.

## Cut over from SSH push — and don't lock yourself out

Once a few deploys have flowed through the pull agent successfully:

1. **Set up a break-glass path BEFORE closing SSH.** If the agent wedges you need a way in.
   Options: Tailscale SSH (normally off), your cloud provider's serial/console access, or a
   firewall rule allowing SSH only from your own IP.
2. Disable the push pipeline: remove the `deploy-production` job from `deploy.yml` (keep the
   build/notification jobs, or retire the file). Delete the `SSH_PRIVATE_KEY`, `SSH_USER`,
   `SERVER_HOST`, `DB_HOST` secrets from the repo.
3. **Close inbound SSH from the internet** at the cloud firewall / security group. The DB
   tunnel CI used is no longer needed either — the app runs Flyway on its own at startup.

## Migrations

The app bundles `spring-boot-starter-flyway` and applies migrations on startup, so the new jar
migrates the DB itself when the agent restarts it — no separate migration step, and the DB
never needs to be reachable from CI. Caveat: a bad migration will fail the health gate and the
helper rolls back the **jar**, but Flyway changes already committed are not auto-reverted. Keep
the hourly DB backup (`restore-db.yml` / `ampairs-db-restore.sh`) as the recovery path.

## Operating notes

- **Logs:** `journalctl -u ampairs-pull-deploy.service` and `/var/log/ampairs/pull-deploy.log`.
- **Force a redeploy:** delete `/opt/ampairs/production/DEPLOYED_COMMIT` and run the agent.
- **Rollback:** the helper keeps `ampairs-service.previous.jar`; on a failed health gate it
  restores it automatically. To roll back manually, re-publish the previous commit's manifest.
- **Silent-failure guard:** because CI no longer controls the deploy, a failed pull is invisible
  to GitHub. `verify-deployment.yml` runs hourly and the agent logs failures — wire an alert on
  the agent's exit status (e.g. `systemd` `OnFailure=` to a notifier) so a stuck deploy pages you.
- **Hardening the trust boundary further (optional):** sign `latest.json` (cosign / GPG) in CI
  and verify the signature in the agent before trusting the checksum — defends against an
  attacker who gains write access to S3 directly rather than through CI.
```
