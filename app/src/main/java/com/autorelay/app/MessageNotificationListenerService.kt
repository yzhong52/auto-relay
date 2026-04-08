package com.autorelay.app

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MessageNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "AutoRelay"
        private const val GOOGLE_MESSAGES_PACKAGE = "com.google.android.apps.messaging"
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        if (sbn.packageName != GOOGLE_MESSAGES_PACKAGE) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: Bundle.EMPTY
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            return
        }

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extractMessageText(extras)

        if (text.isBlank()) {
            Log.d(TAG, "Ignoring Google Messages notification without message text")
            return
        }

        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()

        Log.i(TAG, "─────────────────────────────────────")
        Log.i(TAG, "Incoming message from notification")
        Log.i(TAG, "  Source    : Google Messages notification")
        Log.i(TAG, "  From      : ${title.ifBlank { "Unknown" }}")
        Log.i(TAG, "  Body      : $text")
        if (subText.isNotBlank()) {
            Log.i(TAG, "  Context   : $subText")
        }
        Log.i(TAG, "─────────────────────────────────────")

        val config = RelayConfig(this)
        val notifSender = title.ifBlank { "Unknown" }
        val actions = resolveActions(this, config, notifSender, text)
        RelayLog.add(
            sender = notifSender,
            message = text,
            source = LogEntry.Source.RCS,
            actions = actions
        )
    }

    private fun extractMessageText(extras: Bundle): String {
        val candidates = listOf(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            extras.getCharSequence(Notification.EXTRA_TEXT)
        )
        return candidates
            .firstOrNull { !it.isNullOrBlank() }
            ?.toString()
            ?.trim()
            .orEmpty()
    }

}
