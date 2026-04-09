package com.autorelay.app

import android.content.Context

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
