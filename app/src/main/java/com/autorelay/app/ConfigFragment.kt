package com.autorelay.app

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.telephony.PhoneNumberFormattingTextWatcher
import android.telephony.PhoneNumberUtils
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.autorelay.app.databinding.FragmentConfigBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class ConfigFragment : Fragment() {

    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!

    private lateinit var config: RelayConfig

    private val requestSmsPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            updatePermissionCards()
        }

    private val requestSendSms =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                // Permission just granted — show phone dialog if not yet configured
                if (config.destinationPhone.isBlank()) showPhoneDialog() else {
                    config.smsForwardEnabled = true
                    updateRelayUi()
                }
            }
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
            if (checked && config.destinationEmail.isBlank()) {
                showEmailDialog()
            } else {
                config.relayEnabled = checked
                updateRelayUi()
            }
        }
        binding.tvEmailDestination.setOnClickListener {
            if (config.relayEnabled) showEmailDialog()
        }

        binding.switchSmsEnabled.isChecked = config.smsForwardEnabled
        binding.switchSmsEnabled.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                if (!hasSendSmsPermission()) {
                    binding.switchSmsEnabled.isChecked = false
                    requestSendSms.launch(Manifest.permission.SEND_SMS)
                    return@setOnCheckedChangeListener
                }
                if (config.destinationPhone.isBlank()) {
                    binding.switchSmsEnabled.isChecked = false
                    showPhoneDialog()
                    return@setOnCheckedChangeListener
                }
            }
            config.smsForwardEnabled = checked
            updateRelayUi()
        }
        binding.tvPhoneDestination.setOnClickListener {
            if (config.smsForwardEnabled) showPhoneDialog()
        }

        updatePermissionCards()
        updateRelayUi()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionCards()
    }

    private fun showEmailDialog() {
        val editText = buildInputEditText(
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            hint = getString(R.string.hint_destination_email),
            prefill = config.destinationEmail
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_set_email_title)
            .setView(editText)
            .setPositiveButton(R.string.save) { _, _ ->
                val email = editText.text.toString().trim()
                config.destinationEmail = email
                config.relayEnabled = email.isNotBlank()
                binding.switchRelayEnabled.isChecked = config.relayEnabled
                updateRelayUi()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                binding.switchRelayEnabled.isChecked = config.relayEnabled
            }
            .show()
    }

    private fun showPhoneDialog() {
        val editText = buildInputEditText(
            inputType = InputType.TYPE_CLASS_PHONE,
            hint = getString(R.string.hint_destination_phone),
            prefill = config.destinationPhone
        )
        editText.addTextChangedListener(PhoneNumberFormattingTextWatcher(Locale.getDefault().country))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_set_phone_title)
            .setView(editText)
            .setPositiveButton(R.string.save) { _, _ ->
                val phone = PhoneNumberUtils.normalizeNumber(editText.text.toString().trim())
                config.destinationPhone = phone
                config.smsForwardEnabled = phone.isNotBlank()
                binding.switchSmsEnabled.isChecked = config.smsForwardEnabled
                updateRelayUi()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                binding.switchSmsEnabled.isChecked = config.smsForwardEnabled
            }
            .show()
    }

    private fun buildInputEditText(inputType: Int, hint: String, prefill: String): EditText {
        val padding = (24 * resources.displayMetrics.density).toInt()
        return EditText(requireContext()).apply {
            this.inputType = inputType
            this.hint = hint
            setText(prefill)
            setPadding(padding, paddingTop, padding, paddingBottom)
        }
    }

    private fun hasSendSmsPermission() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED

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
        val emailEnabled = config.relayEnabled
        binding.tvEmailDestination.text = if (emailEnabled && config.destinationEmail.isNotBlank()) {
            config.destinationEmail
        } else {
            getString(R.string.label_destination_not_set)
        }
        binding.tvEmailDestination.alpha = if (emailEnabled) 1f else 0.4f

        val smsEnabled = config.smsForwardEnabled
        binding.tvPhoneDestination.text = if (smsEnabled && config.destinationPhone.isNotBlank()) {
            PhoneNumberUtils.formatNumber(config.destinationPhone, Locale.getDefault().country)
                ?: config.destinationPhone
        } else {
            getString(R.string.label_destination_not_set)
        }
        binding.tvPhoneDestination.alpha = if (smsEnabled) 1f else 0.4f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
