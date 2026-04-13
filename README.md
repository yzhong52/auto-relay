# Auto Relay

An Android app that listens to incoming messages and forwards them to another number or email.

## Screenshots

| Setup | Activity |
|-------|----------|
| ![Setup screen](screenshots/screenshot_20260409-214609.png) | ![Activity screen](screenshots/screenshot_20260409-214614.png) |

## Why

If you use a secondary Android device (or a SIM you don't carry), important messages — OTP codes, 2FA prompts, bank alerts — get stranded on that device. Auto Relay forwards them to wherever you actually are, in real time, so you never miss a verification code because it landed on the wrong phone.

Email forwarding also unlocks agentic workflows: an AI agent monitoring an inbox can read incoming OTPs and 2FA codes and act on them autonomously, without any manual intervention on the phone.

## Features

- Forwards incoming SMS messages to email (via Gmail API + OAuth) or another phone number
- Detects Google Messages notifications as a best-effort fallback for RCS messages
- Deduplicates messages that arrive via both SMS and RCS notification
- Logs all received messages and forwarding outcomes in-app

## RCS Limitations

Android has no public API for third-party apps to receive RCS messages directly. The RCS path works by listening to Google Messages notifications, which has two important limitations:

1. **Sensitive notifications are redacted.** Android allows apps to mark notifications as sensitive, and Google Messages does this for OTPs, verification codes, and bank alerts — exactly the messages you care about. These arrive with the content replaced by "Sensitive notification content hidden" and the sender replaced by "Unknown". Auto Relay detects and discards these silently.

2. **Duplicates with SMS.** Many carriers deliver the same message over both SMS and RCS. When this happens, Auto Relay receives it twice — once via the telephony broadcast and once via the notification. The deduplication logic suppresses the second arrival within a 30-second window.

**In practice:** the SMS path is reliable for OTPs and bank codes. The RCS path adds coverage for messages that arrive only as RCS (e.g. some iMessage-to-Android flows) but will miss sensitive ones.

## Requirements

- Android 8.0+ (API 26+)
- Google Messages installed and set as the default SMS app for the RCS fallback
- A JDK (e.g. [Temurin](https://adoptium.net)) or Android Studio (JDK 17 or 21 recommended)

## Build & Run

Open & build the project in Android Studio, or use the command line:

```sh
# Build and install on a connected device
./gradlew installDebug

# Generate a release APK
./gradlew :app:assembleRelease

# Generate a release App Bundle (AAB) for Play Store
./gradlew :app:bundleRelease

# Stream logs
adb logcat -s AutoRelay
```

For Gmail email forwarding setup, see [Configuring Gmail Email Forwarding](faq/configuring-gmail-email-forwarding.md).

## Beta Testing

To join the beta, request access via the Google group: [auto-relay-testing](https://groups.google.com/g/auto-relay-testing).

## Permissions

- `RECEIVE_SMS` — listen for incoming SMS
- `READ_SMS` — read SMS message content
- `SEND_SMS` — forward messages via SMS
- `INTERNET` — send emails via Gmail API
- Notification access — inspect Google Messages notifications for the RCS fallback

