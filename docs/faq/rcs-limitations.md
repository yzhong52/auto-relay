---
layout: default
title: Why can't Auto Relay forward all RCS messages?
---

Android has no public API for third-party apps to receive RCS messages directly. The RCS path works by listening to Google Messages notifications, which has two important limitations:

1. **Sensitive notifications are redacted.** Android allows apps to mark notifications as sensitive, and Google Messages does this for OTPs, verification codes, and bank alerts — exactly the messages you care about. These arrive with the content replaced by "Sensitive notification content hidden". Auto Relay detects and skips these; they still appear in the in-app log as skipped, but are never forwarded.

2. **Duplicates with SMS.** Many carriers deliver the same message over both SMS and RCS. When this happens, Auto Relay receives it twice — once via the telephony broadcast and once via the notification. The deduplication logic suppresses the second arrival within a 30-second window.

**In practice:** the SMS path is reliable for OTPs and bank codes. The RCS path adds coverage for messages that arrive only as RCS, but will miss sensitive ones.
