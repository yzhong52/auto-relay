package com.autorelay.app.ui

import android.Manifest
import android.util.Log
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.autorelay.app.R
import com.autorelay.app.data.RelayConfig
import com.autorelay.app.databinding.FragmentConfigBinding
import com.autorelay.app.util.hasNotificationListenerAccess
import com.autorelay.app.util.hasSmsPermissions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.api.services.gmail.GmailScopes
import java.util.Locale

class ConfigFragment : Fragment() {

    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!

    private lateinit var config: RelayConfig

    private val requestSendSms =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                if (config.destinationPhoneNumber.isBlank()) showPhoneDialog() else {
                    config.smsForwardEnabled = true
                    updateRelayUi()
                }
            }
        }

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                if (account != null) {
                    config.googleAccountEmail = account.email ?: ""
                    config.emailForwardEnabled = true
                    updateRelayUi()
                }
            } catch (e: Exception) {
                Log.w("AutoRelay", "Google Sign-In failed", e)
                _binding?.let {
                    com.google.android.material.snackbar.Snackbar
                        .make(it.root, R.string.error_sign_in_failed, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .show()
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

        binding.switchRelayEnabled.isChecked = config.emailForwardEnabled
        binding.switchRelayEnabled.setOnCheckedChangeListener { _, checked ->
            if (checked && config.googleAccountEmail.isBlank()) {
                binding.switchRelayEnabled.isChecked = false
                startGoogleSignIn()
            } else {
                config.emailForwardEnabled = checked
                updateRelayUi()
            }
        }
        binding.layoutEmailConfig.setOnClickListener {
            if (config.googleAccountEmail.isBlank()) startGoogleSignIn() else signOutGoogle()
        }

        binding.switchSmsEnabled.isChecked = config.smsForwardEnabled
        binding.switchSmsEnabled.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                if (!hasSendSmsPermission()) {
                    binding.switchSmsEnabled.isChecked = false
                    requestSendSms.launch(Manifest.permission.SEND_SMS)
                    return@setOnCheckedChangeListener
                }
                if (config.destinationPhoneNumber.isBlank()) {
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

    private fun showPhoneDialog() {
        val (container, editText, inputLayout) = buildValidatedInput(
            inputType = InputType.TYPE_CLASS_PHONE,
            hint = getString(R.string.hint_destination_phone),
            prefill = config.destinationPhoneNumber
        )

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_set_phone_title)
            .setView(container)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                binding.switchSmsEnabled.isChecked = config.smsForwardEnabled
            }
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            var isFormatting = false
            editText.doAfterTextChanged { s ->
                if (isFormatting) return@doAfterTextChanged

                val input = s.toString()
                val countryCode = Locale.getDefault().country.ifBlank { "US" }
                val formatted = PhoneNumberUtils.formatNumber(input, countryCode)

                if (formatted != null && formatted != input) {
                    isFormatting = true
                    val selectionStart = editText.selectionStart
                    editText.setText(formatted)
                    if (selectionStart == input.length) {
                        editText.setSelection(formatted.length)
                    } else {
                        editText.setSelection(minOf(selectionStart, formatted.length))
                    }
                    isFormatting = false
                }

                val phone = s.toString().trim()
                val isValid = phone.isEmpty() || android.util.Patterns.PHONE.matcher(phone).matches()
                inputLayout.error = if (isValid) null else getString(R.string.error_invalid_phone)
                saveButton.isEnabled = isValid
            }

            saveButton.setOnClickListener {
                val phone = PhoneNumberUtils.normalizeNumber(editText.text.toString().trim())
                config.destinationPhoneNumber = phone
                config.smsForwardEnabled = phone.isNotBlank()
                binding.switchSmsEnabled.isChecked = config.smsForwardEnabled
                updateRelayUi()
                dialog.dismiss()
            }

            val initialPhone = editText.text.toString().trim()
            saveButton.isEnabled = initialPhone.isEmpty() || android.util.Patterns.PHONE.matcher(initialPhone).matches()
            editText.requestFocus()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        dialog.show()
    }

    private data class DialogInput(val container: View, val editText: EditText, val layout: TextInputLayout)

    private fun buildValidatedInput(inputType: Int, hint: String, prefill: String): DialogInput {
        val context = requireContext()
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            val margin = (24 * resources.displayMetrics.density).toInt()
            setMargins(margin, margin / 2, margin, 0)
        }

        val inputLayout = TextInputLayout(context).apply {
            this.hint = hint
            this.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            this.isErrorEnabled = true
        }

        val editText = TextInputEditText(context).apply {
            this.inputType = inputType
            this.setText(prefill)
            this.isSingleLine = true
        }

        inputLayout.addView(editText)

        val container = FrameLayout(context).apply {
            addView(inputLayout, layoutParams)
        }

        return DialogInput(container, editText, inputLayout)
    }

    private fun hasSendSmsPermission() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED

    private fun updateStatusCard() {
        val hasSms = hasSmsPermissions(requireContext())
        val hasRcs = hasNotificationListenerAccess(requireContext())
        val isActive = hasSms && hasRcs
        val isPartial = hasSms || hasRcs

        val colorRes = if (isActive || isPartial) R.color.status_active else R.color.status_error
        val bgColorRes = if (isActive || isPartial) R.color.status_active_bg else R.color.status_error_bg
        val iconRes = if (isActive || isPartial) R.drawable.ic_status_ok else R.drawable.ic_status_error
        val titleRes = when {
            isActive || isPartial -> R.string.status_active
            else -> R.string.status_inactive
        }
        val descRes = when {
            isActive -> R.string.status_active_desc
            hasSms -> R.string.status_active_sms_only_desc
            hasRcs -> R.string.status_active_rcs_only_desc
            else -> R.string.status_inactive_desc
        }

        binding.tvValueProp.visibility = if (isActive || isPartial) View.GONE else View.VISIBLE

        val statusColor = ContextCompat.getColor(requireContext(), colorRes)
        binding.cardStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), bgColorRes))
        binding.ivServiceStatus.setImageResource(iconRes)
        binding.tvServiceStatus.text = getString(titleRes)
        binding.tvServiceStatus.setTextColor(statusColor)
        binding.tvServiceStatusDesc.text = getString(descRes)
    }

    private fun startGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(GmailScopes.GMAIL_SEND))
            .build()

        val client = GoogleSignIn.getClient(requireActivity(), gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    private fun signOutGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(requireActivity(), gso).signOut().addOnCompleteListener {
            config.googleAccountEmail = ""
            config.emailForwardEnabled = false
            updateRelayUi()
        }
    }

    private fun updateRelayUi() {
        val emailEnabled = config.emailForwardEnabled
        binding.switchRelayEnabled.isChecked = emailEnabled

        if (config.googleAccountEmail.isNotBlank()) {
            binding.tvGoogleAccountStatus.text = getString(R.string.google_account_linked, config.googleAccountEmail)
        } else {
            binding.tvGoogleAccountStatus.text = getString(R.string.google_sign_in_required)
        }

        binding.layoutEmailConfig.alpha = if (emailEnabled) 1f else 0.7f

        val smsEnabled = config.smsForwardEnabled
        binding.tvPhoneDestination.text = if (config.destinationPhoneNumber.isNotBlank()) {
            PhoneNumberUtils.formatNumber(config.destinationPhoneNumber, Locale.getDefault().country)
                ?: config.destinationPhoneNumber
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
