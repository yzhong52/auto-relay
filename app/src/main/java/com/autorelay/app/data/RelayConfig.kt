package com.autorelay.app.data

import android.content.Context
import androidx.core.content.edit

class RelayConfig(context: Context) {
    private val prefs = context.getSharedPreferences("relay_config", Context.MODE_PRIVATE)

    var emailForwardEnabled: Boolean
        get() = prefs.getBoolean(KEY_EMAIL_FORWARD_ENABLED, false)
        set(value) { prefs.edit { putBoolean(KEY_EMAIL_FORWARD_ENABLED, value) } }

    var smsForwardEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMS_ENABLED, false)
        set(value) { prefs.edit { putBoolean(KEY_SMS_ENABLED, value) } }

    var destinationPhoneNumber: String
        get() = prefs.getString(KEY_PHONE, "") ?: ""
        set(value) { prefs.edit { putString(KEY_PHONE, value) } }

    var googleAccountEmail: String
        get() = prefs.getString(KEY_GOOGLE_ACCOUNT, "") ?: ""
        set(value) { prefs.edit { putString(KEY_GOOGLE_ACCOUNT, value) } }

    companion object {
        private const val KEY_EMAIL_FORWARD_ENABLED = "email_forward_enabled"
        private const val KEY_SMS_ENABLED = "sms_forward_enabled"
        private const val KEY_PHONE = "destination_phone_number"
        private const val KEY_GOOGLE_ACCOUNT = "google_account_email"
    }
}
