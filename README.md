# Auto Relay

An Android app that listens to incoming SMS messages and forwards them to another number or email.

## Features

- Receives incoming SMS messages in the background
- Logs sender, body, and timestamp to logcat (tag: `AutoRelay`)
- Requests SMS permissions at runtime

## Requirements

- Android 8.0+ (API 26+)
- A JDK (e.g. [Temurin](https://adoptium.net)) or Android Studio

## Build & Run

### Via Android Studio

Open the project in Android Studio. It will sync Gradle and download dependencies automatically. Connect a device or start an emulator, then click **Run**.

### Via Command Line

```sh
# Install a JDK if needed
brew install --cask temurin

# Build and install on a connected device
./gradlew installDebug

# Launch the app
adb shell am start -n com.autorelay.app/.MainActivity
```

### Viewing SMS Logs

```sh
adb logcat -s AutoRelay
```

## Permissions

The app requires the following permissions (requested at runtime):

- `RECEIVE_SMS` — listen for incoming SMS
- `READ_SMS` — read SMS message content

## Roadmap

- [ ] Forward SMS to another phone number
- [ ] Forward SMS via email
- [ ] Configurable forwarding rules
- [ ] Notification on forward
