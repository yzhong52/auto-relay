# Configuring Gmail Email Forwarding

Only relevant if you are building the app yourself and want to use email forwarding. If you installed the app from the Play Store, sign in with Google and you're done.

Email forwarding requires a one-time Google Cloud setup:

1. Create a project in the [Google Cloud Console](https://console.cloud.google.com/)
2. Enable the **Gmail API** (APIs & Services → Library)
3. Configure the **OAuth consent screen** — set type to External, add your email as a test user
4. Create an OAuth client ID: Credentials → Create Credentials → OAuth client ID → **Android**
   - Package name: `com.autorelay.app`
   - SHA-1: run `keytool -keystore ~/.android/debug.keystore -list -v -storepass android` and copy the SHA-1

No client secret or config file needed — Android OAuth is verified by package name + signing certificate at runtime.
