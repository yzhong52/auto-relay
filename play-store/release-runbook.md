# Play Store Release Runbook

## Prerequisites

- `keystore.properties` present at the repo root (copy from
  `keystore.properties.template` and fill in real values; **never commit
  this file**)
- The keystore JKS file at the path referenced in `keystore.properties`
  (default: `../keystore/auto-relay-release.jks`)
- A JDK installed (`brew install --cask temurin` or Android Studio's
  bundled JDK)
- Access to the [Google Play Console](https://play.google.com/console)

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

1. Go to **Play Console → Auto Relay → Production** (or whichever
   track: Internal / Closed Testing / Open Testing).
2. Click **Create new release**.
3. Upload `app/build/outputs/bundle/release/app-release.aab`.
4. Fill in the **Release notes** (What's new) for each supported locale.
5. Click **Save**, then **Review release**, then **Start rollout**.

---

## 4. Tag the release in git

```sh
git tag v<versionName>
git push origin main --tags
```

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `validateSigningRelease` fails | Check `keystore.properties` path and passwords |
| Upload rejected: version code already exists | Increment `versionCode` |
| Upload rejected: artifact not signed | Ensure `keystore.properties` exists before building |
