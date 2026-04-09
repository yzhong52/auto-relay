# Auto Relay

An Android app that listens to incoming messages and forwards them to another number or email.

## Why

If you use a secondary Android device (or a SIM you don't carry), important messages — OTP codes, 2FA prompts, bank alerts — get stranded on that device. Auto Relay forwards them to wherever you actually are, in real time, so you never miss a verification code because it landed on the wrong phone.

Email forwarding also unlocks agentic workflows: an AI agent monitoring an inbox can read incoming OTPs and 2FA codes and act on them autonomously, without any manual intervention on the phone.

## Features

- Receives incoming SMS messages in the background via `SMS_RECEIVED`
- Detects Google Messages notifications as a fallback for RCS messages
- Logs sender, body, and timestamp to logcat (tag: `AutoRelay`)
- Requests SMS permissions at runtime
- Guides the user to grant notification access for the RCS fallback

## FAQ

**Why does the app use two separate message paths?**
Android has no public API for third-party apps to receive RCS messages directly. SMS arrives via the telephony broadcast; RCS does not. See [faq/why-two-message-paths.md](faq/why-two-message-paths.md) for the full explanation, including limitations and references.

## Requirements

- Android 8.0+ (API 26+)
- Android device notifications enabled for Google Messages if you want the RCS fallback
- A JDK (e.g. [Temurin](https://adoptium.net)) or Android Studio

Recommended JDK:

- JDK 17 or 21 for Gradle/Android Studio compatibility

## Build & Run

### Via Android Studio

Open the project in Android Studio. It will sync Gradle and download dependencies automatically. Connect a device or start an emulator, then click **Run**.

### Configuring the Gmail API (Optional)

To enable email forwarding, you need to provide your own Google Cloud credentials:

1.  Create a project in the [Google Cloud Console](https://console.cloud.google.com/).
2.  Enable the **Gmail API**.
3.  Configure the **OAuth consent screen** and add your email as a **Test User**.
4.  Create two **OAuth client IDs**:
    -   **Android**: Use package name `com.autorelay.app` and your debug SHA-1.
    -   **Web Application**: To be used as the `GMAIL_CLIENT_ID`.
5.  Copy `local.properties.example` to `local.properties` and paste your **Web Client ID**.

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
- `INTERNET` — send emails via Gmail API
- Notification access — inspect Google Messages notifications for the RCS fallback

## Current Architecture

- [SmsReceiver.kt](app/src/main/java/com/autorelay/app/SmsReceiver.kt) processes SMS broadcasts from the telephony stack.
- [MessageNotificationListenerService.kt](app/src/main/java/com/autorelay/app/MessageNotificationListenerService.kt) processes Google Messages notifications for RCS fallback.
- [MainActivity.kt](app/src/main/java/com/autorelay/app/MainActivity.kt) manages SMS permission state and notification access status.

## Roadmap

- [ ] Forward SMS to another phone number
- [ ] Forward RCS-derived notification messages
- [x] Forward SMS via email
- [ ] Configurable forwarding rules
- [ ] Notification on forward
