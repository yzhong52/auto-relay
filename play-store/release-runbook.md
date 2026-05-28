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

1. Open [Play Console → Setup → API access][api-access].
2. Link (or create) a Google Cloud project when prompted.
3. Under **Service accounts**, click **Create new service account**.
4. Follow the Google Cloud link → **Create service account** →
   give it any name (e.g. `auto-relay-ci`) → **Done**.
5. Back in Play Console, click **Grant access** next to the new
   account → Role: **Release manager** → **Invite user**.
6. In Google Cloud Console, open the service account →
   **Keys → Add key → Create new key → JSON** → **Create**.
7. Move the downloaded JSON file to
   `../keystore/play-store-key.json`
   (same folder as the signing keystore — never commit it).
8. Install the Python dependencies once:

```sh
pip3 install google-api-python-client google-auth
```

[api-access]: https://play.google.com/console/developers/api-access

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
automatically. If the file is missing, the build will still succeed but
the bundle will be **unsigned** and cannot be uploaded.

---

## 3. Upload to Play Console

```sh
python3 play-store/upload.py \
  --notes "What's new in this release"
```

The script uploads the AAB and saves the release as a **draft** on the
**internal** track. Optional flags:

| Flag | Default | Description |
|------|---------|-------------|
| `--key` | `../keystore/play-store-key.json` | Service account key |
| `--aab` | `app/build/outputs/bundle/release/app-release.aab` | AAB path |
| `--track` | `internal` | `internal` / `alpha` / `beta` / `production` |
| `--notes` | _(none)_ | What's new text (en-US) |

After the script completes, open Play Console to **review and start
the rollout** (the draft is never auto-promoted to live).

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
| `error: service account key not found` | Complete § 0 setup; verify key path |
| `403 The caller does not have permission` | Grant **Release manager** role in Play Console API access |
