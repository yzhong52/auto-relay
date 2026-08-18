package com.autorelay.app.listener

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.autorelay.app.data.LogEntry
import com.autorelay.app.engine.RelayEngine

class MessageNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "AutoRelay"
        private const val GOOGLE_MESSAGES_PACKAGE = "com.google.android.apps.messaging"

        /** Notification titles that are system/housekeeping — never relay. */
        private val BLOCKED_TITLES = setOf(
            "Device pairing"
        )

        /**
         * Messages' own delivery-status notifications look like title "258
         * messages not sent", text "Check your conversation with X for
         * sending options." These aren't incoming messages, and relaying them
         * as SMS/email can itself trigger further send failures, which
         * re-triggers this same notification — a feedback loop. Both the
         * title and text must match to be filtered, so a coincidental match
         * on just one doesn't drop a real message. The ".+" between "with"
         * and "for sending options" is the contact name — intentionally a
         * wildcard so this matches regardless of who the contact is, rather
         * than hardcoding one name.
         */
        private val SYSTEM_TITLE_PATTERN = Regex("""not sent$""", RegexOption.IGNORE_CASE)
        private val SYSTEM_TEXT_PATTERN =
            Regex("""^check your conversation with .+ for sending options\.?$""", RegexOption.IGNORE_CASE)
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

        if (title in BLOCKED_TITLES) {
            Log.i(TAG, "Skipping notification — blocked title: \"$title\"")
            return
        }

        if (text.isBlank()) {
            return
        }

        val looksLikeDeliveryStatus = SYSTEM_TITLE_PATTERN.containsMatchIn(title) &&
            SYSTEM_TEXT_PATTERN.matches(text)
        if (looksLikeDeliveryStatus) {
            Log.i(TAG, "Skipping notification — looks like a delivery-status notification: title=\"$title\" text=\"$text\"")
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

        val sender = title  // notification title is the closest equivalent to a sender name for RCS
        RelayEngine.processIncomingMessage(this, sender, text, LogEntry.Source.RCS)
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
