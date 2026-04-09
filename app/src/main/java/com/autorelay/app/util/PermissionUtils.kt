package com.autorelay.app.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.autorelay.app.service.MessageNotificationListenerService

fun hasSmsPermissions(context: Context) = listOf(
    Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS
).all {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

fun hasNotificationListenerAccess(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver, "enabled_notification_listeners"
    ) ?: return false
    val expected = ComponentName(
        context, MessageNotificationListenerService::class.java
    ).flattenToString()
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}
