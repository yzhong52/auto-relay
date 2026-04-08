# Auto Relay

An Android app that listens to incoming messages and forwards them to another number or email.

## Features

- Receives incoming SMS messages in the background via `SMS_RECEIVED`
- Detects Google Messages notifications as a fallback for RCS messages
- Logs sender, body, and timestamp to logcat (tag: `AutoRelay`)
- Requests SMS permissions at runtime
- Guides the user to grant notification access for the RCS fallback

## Why There Are Two Message Paths

Android does not expose one single API that covers both SMS and RCS:

- SMS is delivered through the telephony stack, so the app can receive it directly with the `android.provider.Telephony.SMS_RECEIVED` broadcast.
- RCS messages in Google Messages do not trigger that SMS broadcast, so the app cannot observe them through `SmsReceiver` alone.

Because of that, Auto Relay uses two processing paths:

- `SmsReceiver` handles real carrier SMS messages.
- `MessageNotificationListenerService` handles Google Messages notifications, which gives the app a best-effort way to see incoming RCS messages.

This split is intentional. It exists because SMS and RCS arrive through different Android mechanisms.

## Requirements

- Android 8.0+ (API 26+)
- Android device notifications enabled for Google Messages if you want the RCS fallback
- A JDK (e.g. [Temurin](https://adoptium.net)) or Android Studio

Recommended JDK:

- JDK 17 or 21 for Gradle/Android Studio compatibility

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

### Testing

- To test the SMS path, send a real carrier SMS to the Android device.
- To test the RCS path, send a message that arrives in Google Messages as RCS and make sure message notifications are enabled.
- An iPhone-to-Android message may arrive as RCS instead of SMS, so it may only appear through the notification-listener path.

## Permissions

The app uses the following access:

- `RECEIVE_SMS` — listen for incoming SMS
- `READ_SMS` — read SMS message content
- Notification access — inspect Google Messages notifications for the RCS fallback

## Current Architecture

- [SmsReceiver.kt](app/src/main/java/com/autorelay/app/SmsReceiver.kt) processes SMS broadcasts from the telephony stack.
- [MessageNotificationListenerService.kt](app/src/main/java/com/autorelay/app/MessageNotificationListenerService.kt) processes Google Messages notifications for RCS fallback.
- [MainActivity.kt](app/src/main/java/com/autorelay/app/MainActivity.kt) manages SMS permission state and notification access status.

## Roadmap

- [ ] Forward SMS to another phone number
- [ ] Forward RCS-derived notification messages
- [ ] Forward SMS via email
- [ ] Configurable forwarding rules
- [ ] Notification on forward
