# CLAUDE.md

## Project

**Auto Relay** — a native Android app (Kotlin) that intercepts incoming SMS messages and forwards them.

- Package: `com.autorelay.app`
- Min SDK: 26 (Android 8.0), Target/Compile SDK: 34
- Build system: Gradle with Kotlin DSL (`.kts`)
- AGP: 8.2.2, Kotlin: 1.9.22, Gradle: 8.6

## Build

```sh
./gradlew assembleDebug       # build APK
./gradlew installDebug        # build + install on connected device
adb logcat -s AutoRelay       # stream SMS logs
```

Requires a JDK (`brew install --cask temurin`) or use Android Studio which bundles its own.

## Structure

```
app/src/main/
  java/com/autorelay/app/
    MainActivity.kt     # entry point, runtime permission request
    SmsReceiver.kt      # BroadcastReceiver for incoming SMS
  res/
    layout/activity_main.xml
    values/strings.xml, themes.xml, colors.xml
  AndroidManifest.xml   # declares permissions and receiver
```

## Key Conventions

- All SMS log output uses tag `AutoRelay` via `Log.i("AutoRelay", ...)`
- `SmsReceiver` is registered in the manifest with `priority="999"` and `exported="true"`
- Runtime permissions are handled in `MainActivity` using `ActivityResultContracts.RequestMultiplePermissions`
- Prefer small, focused commits scoped to one logical change
