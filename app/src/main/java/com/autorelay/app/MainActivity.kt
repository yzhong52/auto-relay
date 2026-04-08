package com.autorelay.app

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.autorelay.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AutoRelay"
        private val SMS_PERMISSIONS = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
    }

    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                Log.i(TAG, "SMS permissions granted by user")
                onPermissionsGranted()
            } else {
                Log.w(TAG, "SMS permissions denied by user")
                onPermissionsDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGrantPermission.setOnClickListener {
            requestSmsPermissions()
        }
        binding.btnNotificationAccess.setOnClickListener {
            openNotificationAccessSettings()
        }

        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun checkAndRequestPermissions() {
        if (hasSmsPermissions()) {
            Log.i(TAG, "SMS permissions already granted")
        } else {
            Log.i(TAG, "Requesting SMS permissions")
            requestSmsPermissions()
        }

        updateUi()
    }

    private fun hasSmsPermissions(): Boolean {
        return SMS_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestSmsPermissions() {
        requestPermissionLauncher.launch(SMS_PERMISSIONS)
    }

    private fun onPermissionsGranted() {
        Log.d(TAG, "AutoRelay is active and listening for SMS messages")
        updateUi()
    }

    private fun onPermissionsDenied() {
        Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
        Log.w(TAG, "AutoRelay cannot function without SMS permissions")
        updateUi()
    }

    private fun updateUi() {
        val hasSms = hasSmsPermissions()
        val hasNotificationAccess = hasNotificationListenerAccess()

        binding.tvStatus.text = getString(
            R.string.status_summary,
            if (hasSms) getString(R.string.status_enabled) else getString(R.string.status_missing),
            if (hasNotificationAccess) getString(R.string.status_enabled) else getString(R.string.status_missing)
        )

        binding.btnGrantPermission.visibility = if (hasSms) View.GONE else View.VISIBLE
        binding.btnNotificationAccess.visibility = if (hasNotificationAccess) View.GONE else View.VISIBLE
    }

    private fun hasNotificationListenerAccess(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val expectedComponent = ComponentName(this, MessageNotificationListenerService::class.java)
            .flattenToString()

        return enabledListeners.split(':').any { it.equals(expectedComponent, ignoreCase = true) }
    }

    private fun openNotificationAccessSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
}
