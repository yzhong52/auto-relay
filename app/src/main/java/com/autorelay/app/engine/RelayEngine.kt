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

        if (config.emailForwardEnabled) {
            if (config.destinationEmail.isNotBlank()) {
                actions.add(context.getString(R.string.action_forwarded, config.destinationEmail))
            } else {
                actions.add(context.getString(R.string.action_no_email))
            }
        }

        if (config.smsForwardEnabled) {
            if (config.destinationPhoneNumber.isNotBlank()) {
                val displayPhone = PhoneNumberUtils.formatNumber(config.destinationPhoneNumber, Locale.getDefault().country)
                    ?: config.destinationPhoneNumber
                actions.add(context.getString(R.string.action_sms_forwarded, displayPhone))
            } else {
                actions.add(context.getString(R.string.action_no_phone))
            }
        }

        if (actions.isEmpty()) {
            actions.add(context.getString(R.string.action_relay_disabled))
        }

        // Add the initial log entry
        RelayLog.add(
            sender = sender,
            message = body,
            source = source,
            actions = actions
        )

        // Execute background tasks
        Thread {
            if (config.emailForwardEnabled && config.destinationEmail.isNotBlank()) {
                forwardToEmail(context, config.destinationEmail, sender, body)
            }
            if (config.smsForwardEnabled && config.destinationPhoneNumber.isNotBlank()) {
                forwardToSms(context, config.destinationPhoneNumber, sender, body)
            }
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
