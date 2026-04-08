package com.autorelay.app

import android.content.Context
import android.telephony.SmsManager

fun resolveActions(context: Context, config: RelayConfig, sender: String, body: String): List<String> {
    val actions = mutableListOf<String>()

    if (config.relayEnabled) {
        if (config.destinationEmail.isBlank()) {
            actions.add(context.getString(R.string.action_no_email))
        } else {
            // TODO: implement email sending
            actions.add(context.getString(R.string.action_forwarded, config.destinationEmail))
        }
    }

    if (config.smsForwardEnabled) {
        if (config.destinationPhone.isBlank()) {
            actions.add(context.getString(R.string.action_no_phone))
        } else {
            sendSms(context, config.destinationPhone, sender, body)
            actions.add(context.getString(R.string.action_sms_forwarded, config.destinationPhone))
        }
    }

    if (actions.isEmpty()) {
        actions.add(context.getString(R.string.action_relay_disabled))
    }

    return actions
}

private fun sendSms(context: Context, destination: String, sender: String, body: String) {
    val smsManager = context.getSystemService(SmsManager::class.java)
    val fullMessage = "From: $sender\n$body"
    val parts = smsManager.divideMessage(fullMessage)
    if (parts.size == 1) {
        smsManager.sendTextMessage(destination, null, fullMessage, null, null)
    } else {
        smsManager.sendMultipartTextMessage(destination, null, parts, null, null)
    }
}

class RelayConfig(context: Context) {
    private val prefs = context.getSharedPreferences("relay_config", Context.MODE_PRIVATE)

    var relayEnabled: Boolean
        get() = prefs.getBoolean(KEY_EMAIL_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_EMAIL_ENABLED, value).apply() }

    var destinationEmail: String
        get() = prefs.getString(KEY_EMAIL, "") ?: ""
        set(value) { prefs.edit().putString(KEY_EMAIL, value).apply() }

    var smsForwardEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMS_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_SMS_ENABLED, value).apply() }

    var destinationPhone: String
        get() = prefs.getString(KEY_PHONE, "") ?: ""
        set(value) { prefs.edit().putString(KEY_PHONE, value).apply() }

    companion object {
        private const val KEY_EMAIL_ENABLED = "relay_enabled"
        private const val KEY_EMAIL = "destination_email"
        private const val KEY_SMS_ENABLED = "sms_forward_enabled"
        private const val KEY_PHONE = "destination_phone"
    }
}
