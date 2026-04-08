package com.autorelay.app

import android.content.Context

fun resolveActions(context: Context, config: RelayConfig): List<String> = when {
    !config.relayEnabled -> listOf(context.getString(R.string.action_relay_disabled))
    config.destinationEmail.isBlank() -> listOf(context.getString(R.string.action_no_email))
    else -> listOf(context.getString(R.string.action_forwarded, config.destinationEmail))
}

class RelayConfig(context: Context) {
    private val prefs = context.getSharedPreferences("relay_config", Context.MODE_PRIVATE)

    var relayEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_ENABLED, value).apply() }

    var destinationEmail: String
        get() = prefs.getString(KEY_EMAIL, "") ?: ""
        set(value) { prefs.edit().putString(KEY_EMAIL, value).apply() }

    companion object {
        private const val KEY_ENABLED = "relay_enabled"
        private const val KEY_EMAIL = "destination_email"
    }
}
