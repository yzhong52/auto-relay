---
layout: default
title: Do I need to keep the app open for relaying to work?
---

No. Once permissions are granted and forwarding is configured, the app works entirely in the background.

- **SMS** — `SmsReceiver` is declared in the manifest, so Android wakes the app process to handle incoming SMS broadcasts even when the app is closed. The `RECEIVE_SMS` broadcast is also exempt from Doze mode restrictions.
- **RCS** — `MessageNotificationListenerService` is a persistent background service that Android keeps running once notification access is granted.

The app only needs to be opened once for initial setup.
