package com.autorelay.app

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.autorelay.app.databinding.FragmentConfigBinding

class ConfigFragment : Fragment() {

    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!

    private lateinit var config: RelayConfig

    private val requestSmsPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            updatePermissionCards()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        config = RelayConfig(requireContext())

        binding.btnGrantSms.setOnClickListener {
            requestSmsPermissions.launch(
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
            )
        }
        binding.btnGrantNotification.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        binding.switchRelayEnabled.isChecked = config.relayEnabled
        binding.switchRelayEnabled.setOnCheckedChangeListener { _, checked ->
            config.relayEnabled = checked
            updateRelayUi()
        }

        binding.etEmail.setText(config.destinationEmail)
        binding.btnSaveEmail.setOnClickListener {
            config.destinationEmail = binding.etEmail.text?.toString()?.trim() ?: ""
            binding.btnSaveEmail.text = getString(R.string.saved)
            binding.btnSaveEmail.postDelayed({
                if (_binding != null) binding.btnSaveEmail.text = getString(R.string.save)
            }, 1500)
        }

        updatePermissionCards()
        updateRelayUi()
    }

    override fun onResume() {
        super.onResume()
        // Re-check notification access in case user granted it from Settings and returned
        updatePermissionCards()
    }

    private fun hasSmsPermissions() = listOf(
        Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS
    ).all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasNotificationListenerAccess(): Boolean {
        val enabled = Settings.Secure.getString(
            requireContext().contentResolver, "enabled_notification_listeners"
        ) ?: return false
        val expected = ComponentName(
            requireContext(), MessageNotificationListenerService::class.java
        ).flattenToString()
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun updatePermissionCards() {
        val hasSms = hasSmsPermissions()
        binding.ivSmsStatus.setImageResource(
            if (hasSms) R.drawable.ic_status_ok else R.drawable.ic_status_error
        )
        binding.tvSmsStatus.text = getString(
            if (hasSms) R.string.sms_permission_granted else R.string.sms_permission_missing
        )
        binding.btnGrantSms.visibility = if (hasSms) View.GONE else View.VISIBLE

        val hasNotif = hasNotificationListenerAccess()
        binding.ivNotifStatus.setImageResource(
            if (hasNotif) R.drawable.ic_status_ok else R.drawable.ic_status_error
        )
        binding.tvNotifStatus.text = getString(
            if (hasNotif) R.string.notif_access_granted else R.string.notif_access_missing
        )
        binding.btnGrantNotification.visibility = if (hasNotif) View.GONE else View.VISIBLE
    }

    private fun updateRelayUi() {
        binding.tilEmail.isEnabled = config.relayEnabled
        binding.btnSaveEmail.isEnabled = config.relayEnabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
