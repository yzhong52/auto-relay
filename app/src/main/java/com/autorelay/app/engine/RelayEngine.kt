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

    fun processIncomingMessage(
        context: Context,
        sender: String,
        body: String,
        source: LogEntry.Source
    ): List<String> {
        val config = RelayConfig(context)
        val actions = mutableListOf<String>()

        if (config.relayEnabled) {
            if (config.destinationEmail.isBlank()) {
                actions.add(context.getString(R.string.action_no_email))
            } else {
                val success = forwardToEmail(config.destinationEmail, sender, body)
                if (success) {
                    actions.add(context.getString(R.string.action_forwarded, config.destinationEmail))
                } else {
                    actions.add("Email relay failed (check network)")
                }
            }
        }

        if (config.smsForwardEnabled) {
            if (config.destinationPhone.isBlank()) {
                actions.add(context.getString(R.string.action_no_phone))
            } else {
                val success = forwardToSms(context, config.destinationPhone, sender, body)
                if (success) {
                    val displayPhone = PhoneNumberUtils.formatNumber(config.destinationPhone, Locale.getDefault().country)
                        ?: config.destinationPhone
                    actions.add(context.getString(R.string.action_sms_forwarded, displayPhone))
                } else {
                    actions.add("SMS relay failed")
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

    private fun forwardToEmail(destination: String, originalSender: String, body: String): Boolean {
        // TODO: Implement actual email sending (e.g. SMTP or HTTP API)
        Log.i(TAG, "Email relay not yet implemented (destination=$destination)")
        return false
    }
}
