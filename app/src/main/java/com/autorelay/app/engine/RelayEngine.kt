package com.autorelay.app.engine

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.util.Log
import com.autorelay.app.R
import com.autorelay.app.data.LogEntry
import com.autorelay.app.data.RelayConfig
import com.autorelay.app.data.RelayLog
import java.util.Locale

/**
 * Handles the actual execution of relaying messages.
 */
object RelayEngine {
    private const val TAG = "RelayEngine"
    private const val DEDUP_WINDOW_MS = 30_000L
    private val NOISE_PATTERNS = listOf(
        "sensitive notification content hidden",
        "is doing work in the background"
    )
    private val recentBodyHashes = mutableListOf<Pair<Int, Long>>()

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
                RelayLog.add(sender, body, source, listOf(context.getString(R.string.action_skipped_unknown_sender)))
                return@Thread
            }

            if (NOISE_PATTERNS.any { body.contains(it, ignoreCase = true) }) {
                RelayLog.add(sender, body, source, listOf(context.getString(R.string.action_skipped_sensitive)))
                return@Thread
            }

            if (config.emailForwardEnabled) {
                if (config.destinationEmail.isBlank()) {
                    actions.add(context.getString(R.string.action_no_email))
                } else {
                    val success = forwardToEmail(context, config.destinationEmail, sender, body)
                    actions.add(
                        if (success) context.getString(R.string.action_forwarded, config.destinationEmail)
                        else context.getString(R.string.action_email_failed)
                    )
                }
            }

            if (config.smsForwardEnabled) {
                if (config.destinationPhoneNumber.isBlank()) {
                    actions.add(context.getString(R.string.action_no_phone))
                } else {
                    val success = forwardToSms(context, config.destinationPhoneNumber, sender, body)
                    if (success) {
                        val displayPhone = PhoneNumberUtils.formatNumber(config.destinationPhoneNumber, Locale.getDefault().country)
                            ?: config.destinationPhoneNumber
                        actions.add(context.getString(R.string.action_sms_forwarded, displayPhone))
                    } else {
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
            if (parts.size == 1) {
                smsManager.sendTextMessage(destination, null, fullMessage, null, null)
            } else {
                smsManager.sendMultipartTextMessage(destination, null, parts, null, null)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding SMS", e)
            false
        }
    }

    private fun forwardToEmail(context: Context, destination: String, originalSender: String, body: String): Boolean {
        val subject = "Forwarded message from $originalSender"
        val bodyText = "From: $originalSender\n\n$body"
        return GmailProvider.sendEmail(context, destination, subject, bodyText)
    }
}
