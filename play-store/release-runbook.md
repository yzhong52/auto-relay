# Play Store Release Runbook

## Prerequisites

- `keystore.properties` present at the repo root (copy from
  `keystore.properties.template` and fill in real values; **never commit
  this file**)
- The keystore JKS file at the path referenced in `keystore.properties`
  (default: `../keystore/auto-relay-release.jks`)
- A JDK installed (`brew install --cask temurin` or Android Studio's
  bundled JDK)
- A Play Store service account key at
  `../keystore/play-store-key.json` (one-time setup — see § 0 below)

---

## 0. One-time: set up the Play Store service account

> Skip this once `../keystore/play-store-key.json` already exists.

### 0a. Create the service account

1. Open Google Cloud Console for the project linked to your Play
   Console account:
   [https://console.cloud.google.com/iam-admin/serviceaccounts?project=auto-relay-app](https://console.cloud.google.com/iam-admin/serviceaccounts?project=auto-relay-app)
2. Click **Create service account**.
3. Name it `auto-relay-ci` (ID auto-fills) → **Create and continue**.
4. Skip the optional permission and user-access steps → **Done**.
5. Click the new service account → **Keys** tab →
   **Add Key → Create new key → JSON → Create**.
6. Move the downloaded file to `../keystore/play-store-key.json`
   (same folder as the signing keystore — **never commit it**).

### 0b. Enable the Play Developer API

Enable the Google Play Android Developer API for your GCP project:
[https://console.cloud.google.com/apis/library/androidpublisher.googleapis.com?project=auto-relay-app](https://console.cloud.google.com/apis/library/androidpublisher.googleapis.com?project=auto-relay-app)

### 0c. Grant Play Console access

1. Open **Play Console → Users and permissions → Invite new user**.
2. Enter the service account email
   (`auto-relay-ci@auto-relay-app.iam.gserviceaccount.com`).
3. Under **Releases**, enable all three permissions:
   - Release to production, exclude devices, and use Play App Signing
   - Release apps to testing tracks
   - Manage testing tracks and edit tester lists
4. Click **Invite user**.

### 0d. Install Python dependencies (once)

```sh
pip3 install google-api-python-client google-auth
```

---

## 1. Bump version

In `app/build.gradle.kts`:

```kotlin
versionCode = <previous + 1>   // integer, must strictly increase
versionName = "<major.minor.patch>"
```

Commit the change:

```sh
git add app/build.gradle.kts
git commit -m "Bump version to <versionName> (<versionCode>)"
```

---

## 2. Build the signed release AAB

```sh
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

The build reads signing credentials from `keystore.properties`
automatically. If the file is missing, the build will still succeed
but the bundle will be **unsigned** and cannot be uploaded.

---

## 3. Upload to Play Console

### 3a. Automated upload (TODO — not yet working)

The script `play-store/upload.py` is intended to automate this step:

```sh
python3 play-store/upload.py \
  --notes "What's new in this release"
```

Optional flags:

| Flag | Default | Description |
|------|---------|-------------|
| `--key` | `../keystore/play-store-key.json` | Service account key |
| `--aab` | `app/build/outputs/bundle/release/app-release.aab` | AAB path |
| `--track` | `internal` | `internal` / `alpha` / `beta` / `production` |
| `--notes` | _(none)_ | What's new text (en-US) |

Steps 0a–0d were completed but the script returns
`403 The caller does not have permission`. The root cause is likely
that the GCP project is not linked to Play Console — the "API access"
page needed to do this was not reachable in the Play Console UI.
Further investigation required.

### 3b. Manual upload

1. Go to **Play Console → Auto Relay → your track**.
2. Click **Create new release**.
3. Upload `app/build/outputs/bundle/release/app-release.aab`.
4. Fill in release notes → **Save → Review release → Start rollout**.

---

## 4. Tag the release in git

```sh
git tag v<versionName>
git push origin main --tags
```

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `validateSigningRelease` fails | Check `keystore.properties` path and passwords |
| Upload rejected: version code already exists | Increment `versionCode` |
| Upload rejected: artifact not signed | Ensure `keystore.properties` exists before building |
| `error: service account key not found` | Complete § 0a; verify key is at `../keystore/play-store-key.json` |
| `403 … SERVICE_DISABLED` | Complete § 0b to enable the Play Developer API |
| `403 The caller does not have permission` | GCP project may not be linked to Play Console — see TODO in § 3 |