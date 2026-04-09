# Auto Relay

An Android app that listens to incoming messages and forwards them to another number or email.

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

### Via Android Studio

Open the project in Android Studio. It will sync Gradle and download dependencies automatically. Connect a device or start an emulator, then click **Run**.

### Via Command Line

```sh
# Install a JDK if needed
brew install --cask temurin

# Build and install on a connected device
./gradlew installDebug

# Stream logs
adb logcat -s AutoRelay
```

## Configuring Gmail Email Forwarding

Email forwarding requires a one-time Google Cloud setup:

1. Create a project in the [Google Cloud Console](https://console.cloud.google.com/)
2. Enable the **Gmail API** (APIs & Services → Library)
3. Configure the **OAuth consent screen** — set type to External, add your email as a test user
4. Create an OAuth client ID: Credentials → Create Credentials → OAuth client ID → **Android**
   - Package name: `com.autorelay.app`
   - SHA-1: run `keytool -keystore ~/.android/debug.keystore -list -v -storepass android` and copy the SHA-1

No client secret or config file needed — Android OAuth is verified by package name + signing certificate at runtime.

## Permissions

- `RECEIVE_SMS` — listen for incoming SMS
- `READ_SMS` — read SMS message content
- `SEND_SMS` — forward messages via SMS
- `INTERNET` — send emails via Gmail API
- Notification access — inspect Google Messages notifications for the RCS fallback

