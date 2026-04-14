# Configuring Gmail Email Forwarding

Email forwarding allows the app to send your messages to your inbox. 

If you're using the version from the Play Store, simply **Sign In** with your Google account in the app and you're all set.

### For Developers (Self-Building)

If you are building the app from source code, you'll need to set up your own Google Cloud project to enable the Gmail API:

1. **Enable Gmail API**: Go to the [Google Cloud Console](https://console.cloud.google.com/), create a project, and enable the **Gmail API** in the Library.
2. **Setup OAuth**: Configure the **OAuth consent screen** (set to "External") and add your email as a "Test User."
3. **Register your App**: Create an **Android OAuth client ID**:
   - Use `com.autorelay.app` as the package name.
   - For the SHA-1 fingerprint, run: `keytool -keystore ~/.android/debug.keystore -list -v -storepass android` and copy the SHA-1 code.

That's it! No extra config files are needed—Google verifies your app automatically using your package name and certificate.
