package com.autorelay.app.engine

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.autorelay.app.R
import com.autorelay.app.data.LogEntry
import com.autorelay.app.data.RelayConfig
import com.autorelay.app.data.RelayLog
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Handles the actual execution of relaying messages.
 */
object RelayEngine {
    private const val TAG = "RelayEngine"
    private const val DEDUP_WINDOW_MS = 30_000L
    private const val SMS_SENT_ACTION = "com.autorelay.app.action.SMS_FORWARD_SENT"
    private const val SMS_SENT_TIMEOUT_MS = 15_000L

    /**
     * After this many consecutive SMS forward failures, stop attempting new
     * sends until one succeeds again. Without this, a broken destination
     * (no service, no SIM, etc.) makes every incoming message trigger a new
     * failed send — and each failure can itself surface as a new Messages
     * notification that gets relayed, and fails, and re-notifies...
     */
    private const val SMS_CIRCUIT_BREAKER_THRESHOLD = 5

    private val NOISE_PATTERNS = listOf(
        "sensitive notification content hidden",
        "messages is doing work in the background",
        "Your messages are available on the device you've paired"
    )
    private val recentBodyHashes = mutableListOf<Pair<Int, Long>>()
    private val smsSendRequestCode = AtomicInteger(0)
    private val consecutiveSmsFailures = AtomicInteger(0)

    @Synchronized
    private fun isDuplicate(body: String): Boolean {
        val now = System.currentTimeMillis()
        val hash = body.trim().hashCode()
        recentBodyHashes.removeAll { now - it.second > DEDUP_WINDOW_MS }
        return if (recentBodyHashes.any { it.first == hash }) {
            true
        } else {
            recentBodyHashes.add(hash to now)
            false
        }
    }

    fun processIncomingMessage(
        context: Context,
        sender: String,
        body: String,
        source: LogEntry.Source
    ): List<String> {
        val config = RelayConfig(context)
        val actions = mutableListOf<String>()

        Thread {
            if (isDuplicate(body)) {
                Log.d(TAG, "Duplicate message detected — already processed via another path, skipping")
                return@Thread
            }

            if (sender.isBlank()) {
                RelayLog.add(LogEntry.UNKNOWN_SENDER, body, source, listOf(context.getString(R.string.action_skipped_unknown_sender)))
                return@Thread
            }

            if (NOISE_PATTERNS.any { body.contains(it, ignoreCase = true) }) {
                RelayLog.add(sender, body, source, listOf(context.getString(R.string.action_skipped_sensitive)))
                return@Thread
            }

            if (config.emailForwardEnabled) {
                val success = forwardToEmail(context, config.googleAccountEmail, sender, body)
                actions.add(
                    if (success) context.getString(R.string.action_forwarded, config.googleAccountEmail)
                    else context.getString(R.string.action_email_failed)
                )
            }

            if (config.smsForwardEnabled) {
                if (config.destinationPhoneNumber.isBlank()) {
                    actions.add(context.getString(R.string.action_no_phone))
                } else if (consecutiveSmsFailures.get() >= SMS_CIRCUIT_BREAKER_THRESHOLD) {
                    Log.w(TAG, "SMS forwarding circuit breaker open after $SMS_CIRCUIT_BREAKER_THRESHOLD consecutive failures — skipping send")
                    actions.add(context.getString(R.string.action_sms_circuit_open))
                } else {
                    val success = forwardToSms(context, config.destinationPhoneNumber, sender, body)
                    if (success) {
                        consecutiveSmsFailures.set(0)
                        val displayPhone = PhoneNumberUtils.formatNumber(config.destinationPhoneNumber, Locale.getDefault().country)
                            ?: config.destinationPhoneNumber
                        actions.add(context.getString(R.string.action_sms_forwarded, displayPhone))
                    } else {
                        consecutiveSmsFailures.incrementAndGet()
                        actions.add(context.getString(R.string.action_sms_failed))
                    }
                }
            }

            if (actions.isEmpty()) {
                actions.add(context.getString(R.string.action_relay_disabled))
            }

            RelayLog.add(
                sender = sender,
                message = body,
                source = source,
                actions = actions
            )
        }.start()

        return actions
    }

    private fun forwardToSms(context: Context, destination: String, originalSender: String, body: String): Boolean {
        return try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            val fullMessage = "From: $originalSender\n$body"
            val parts = smsManager.divideMessage(fullMessage)
            awaitSendResult(context, parts.size) { sentIntent ->
                if (parts.size == 1) {
                    smsManager.sendTextMessage(destination, null, fullMessage, sentIntent, null)
                } else {
                    val sentIntents = ArrayList<PendingIntent>(parts.size).apply {
                        repeat(parts.size) { add(sentIntent) }
                    }
                    smsManager.sendMultipartTextMessage(destination, null, parts, sentIntents, null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding SMS", e)
            false
        }
    }

    /**
     * Sends via [send] and blocks (this always runs off the main thread —
     * see the Thread in [processIncomingMessage]) until every part's
     * PendingIntent reports back, or [SMS_SENT_TIMEOUT_MS] elapses. A plain
     * try/catch around sendTextMessage can't detect failure: that call only
     * throws on a malformed request, not on RESULT_ERROR_NO_SERVICE,
     * RESULT_ERROR_RADIO_OFF, etc. — those are only reported via sentIntent.
     */
    private fun awaitSendResult(context: Context, partCount: Int, send: (PendingIntent) -> Unit): Boolean {
        val allOk = AtomicBoolean(true)
        val latch = CountDownLatch(partCount)
        val action = "$SMS_SENT_ACTION.${smsSendRequestCode.incrementAndGet()}"

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                if (resultCode != Activity.RESULT_OK) {
                    allOk.set(false)
                    Log.w(TAG, "SMS forward part failed with result code $resultCode")
                }
                latch.countDown()
            }
        }

        ContextCompat.registerReceiver(context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)
        try {
            val sentIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(action).setPackage(context.packageName),
                PendingIntent.FLAG_IMMUTABLE
            )
            send(sentIntent)

            if (!latch.await(SMS_SENT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                Log.w(TAG, "Timed out waiting for SMS send result")
                allOk.set(false)
            }
        } finally {
            context.unregisterReceiver(receiver)
        }

        return allOk.get()
    }

    private fun forwardToEmail(context: Context, destination: String, originalSender: String, body: String): Boolean {
        val subject = "[AutoRelay] ($deviceName) Forwarded message from $originalSender"
        val bodyText = "From: $originalSender\n\n$body"
        return GmailProvider.sendEmail(context, destination, subject, bodyText)
    }

    private val deviceName: String
        get() = Build.MODEL ?: "Unknown device"
}
