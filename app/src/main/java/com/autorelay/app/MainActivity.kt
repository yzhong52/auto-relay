package com.autorelay.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        if (hasSmsPermissions()) {
            Log.i(TAG, "SMS permissions already granted")
            onPermissionsGranted()
        } else {
            Log.i(TAG, "Requesting SMS permissions")
            requestSmsPermissions()
        }
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
        binding.tvStatus.text = getString(R.string.status_running)
        binding.btnGrantPermission.visibility = View.GONE
        Log.d(TAG, "AutoRelay is active and listening for SMS messages")
    }

    private fun onPermissionsDenied() {
        binding.tvStatus.text = getString(R.string.permission_required)
        binding.btnGrantPermission.visibility = View.VISIBLE
        Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
        Log.w(TAG, "AutoRelay cannot function without SMS permissions")
    }
}
