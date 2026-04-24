---
layout: default
title: Why does Auto Relay use two message paths instead of one?
---

# Why does Auto Relay use two message paths instead of one?

Android does not provide a single API that covers both SMS and RCS, so the app has to handle each through a different mechanism.

## The SMS path

`SmsReceiver` listens for `android.provider.Telephony.SMS_RECEIVED`. This is the standard telephony broadcast that Android has supported for years. Any third-party app with `RECEIVE_SMS` permission can use it.

## The RCS path

RCS messages in Google Messages **do not trigger `SMS_RECEIVED`**. They go through an entirely separate path in the system and are never delivered as SMS PDUs, so `SmsReceiver` never sees them.

Android does include `ImsRcsManager` (added in API 30), but it is restricted to system apps and whitelisted vendors — over 30 methods are marked `@hide` in AOSP. There is no public API that lets a third-party app receive incoming RCS messages directly.

Because of that, `MessageNotificationListenerService` intercepts Google Messages notifications as a best-effort fallback. This is the same approach used by other third-party SMS/RCS apps (Textra, Pulse SMS, etc.). It is not a workaround unique to Auto Relay — it is the only door Android leaves open.

## Known limitations of the notification fallback

- Only captures messages when Google Messages posts a visible notification. Messages in silenced or muted threads will be missed.
- Notification text may be truncated for very long messages. The service reads `EXTRA_BIG_TEXT` first, then falls back to `EXTRA_TEXT`, but neither is guaranteed to be the full message body.
- The service is hardcoded to `com.google.android.apps.messaging`. If the user's default RCS app is Samsung Messages (`com.samsung.android.messaging`) or another client, those messages will not be captured.
- Requires the user to explicitly grant notification access, which is a broader permission than SMS and some users will decline it.

## References

- [Android Developers — RCS Google Messages archival (enterprise API)](https://developer.android.com/work/dpc/rcs-messages-archival)
- [Android Developers — ImsRcsManager](https://developer.android.com/reference/android/telephony/ims/ImsRcsManager)
- [XDA Developers — Google Messages has a hidden RCS API for third-party apps (Samsung only)](https://www.xda-developers.com/google-messages-rcs-api-third-party-apps/)
- [9to5Google — Android prepping more RCS APIs for OEMs, not third-party apps](https://9to5google.com/2019/07/30/android-rcs-apis-oems-not-third-party-apps/)
- [AOSP — Implement IMS](https://source.android.com/docs/core/connect/ims)
