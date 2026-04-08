package com.autorelay.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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

    private val requestSendSms =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
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

        binding.cardStatus.setOnClickListener { openPermissions() }

        binding.switchRelayEnabled.isChecked = config.relayEnabled
        binding.switchRelayEnabled.setOnCheckedChangeListener { _, checked ->
            if (checked && config.destinationEmail.isBlank()) {
                showEmailDialog()
            } else {
                config.relayEnabled = checked
                updateRelayUi()
            }
        }
        binding.layoutEmailConfig.setOnClickListener { showEmailDialog() }

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
        binding.layoutSmsConfig.setOnClickListener { showPhoneDialog() }

        updateStatusCard()
        updateRelayUi()

        // Auto-navigate to permissions on first launch if anything is missing
        if (savedInstanceState == null && !allPermissionsGranted()) {
            openPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatusCard()
    }

    private fun openPermissions() {
        (requireActivity() as MainActivity).openPermissions()
    }

    private fun allPermissionsGranted() =
        hasSmsPermissions(requireContext()) && hasNotificationListenerAccess(requireContext())

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

    private fun updateStatusCard() {
        val isActive = allPermissionsGranted()
        val statusColor = ContextCompat.getColor(requireContext(),
            if (isActive) R.color.status_active else R.color.status_error)
        val statusBgColor = ContextCompat.getColor(requireContext(),
            if (isActive) R.color.status_active_bg else R.color.status_error_bg)
        binding.cardStatus.setCardBackgroundColor(statusBgColor)
        binding.ivServiceStatus.setImageResource(
            if (isActive) R.drawable.ic_status_ok else R.drawable.ic_status_error)
        binding.tvServiceStatus.text = getString(
            if (isActive) R.string.status_active else R.string.status_inactive)
        binding.tvServiceStatus.setTextColor(statusColor)
        binding.tvServiceStatusDesc.text = getString(
            if (isActive) R.string.status_active_desc else R.string.status_inactive_desc)
    }

    private fun updateRelayUi() {
        val emailEnabled = config.relayEnabled
        binding.tvEmailDestination.text = if (config.destinationEmail.isNotBlank()) {
            config.destinationEmail
        } else {
            getString(R.string.label_destination_not_set)
        }
        binding.tvEmailDestination.alpha = if (emailEnabled) 1f else 0.5f
        binding.layoutEmailConfig.alpha = if (emailEnabled) 1f else 0.7f

        val smsEnabled = config.smsForwardEnabled
        binding.tvPhoneDestination.text = if (config.destinationPhone.isNotBlank()) {
            PhoneNumberUtils.formatNumber(config.destinationPhone, Locale.getDefault().country)
                ?: config.destinationPhone
        } else {
            getString(R.string.label_destination_not_set)
        }
        binding.tvPhoneDestination.alpha = if (smsEnabled) 1f else 0.5f
        binding.layoutSmsConfig.alpha = if (smsEnabled) 1f else 0.7f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
