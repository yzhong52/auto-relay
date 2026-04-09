package com.autorelay.app.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.autorelay.app.R
import com.autorelay.app.databinding.FragmentPermissionsBinding
import com.autorelay.app.util.hasNotificationListenerAccess
import com.autorelay.app.util.hasSmsPermissions

class PermissionsFragment : Fragment() {

    private var _binding: FragmentPermissionsBinding? = null
    private val binding get() = _binding!!

    private val requestSmsPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            updatePermissionCards()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPermissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnGrantSms.setOnClickListener {
            requestSmsPermissions.launch(
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
            )
        }
        binding.btnGrantNotification.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        updatePermissionCards()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionCards()
    }

    private fun updatePermissionCards() {
        val hasSms = hasSmsPermissions(requireContext())
        val okColor = ContextCompat.getColor(requireContext(), R.color.status_active)
        val errorColor = ContextCompat.getColor(requireContext(), R.color.status_error)

        binding.ivSmsStatus.setImageResource(
            if (hasSms) R.drawable.ic_status_ok else R.drawable.ic_status_error
        )
        binding.tvSmsStatus.text = getString(
            if (hasSms) R.string.sms_permission_granted else R.string.sms_permission_missing
        )
        binding.tvSmsStatus.setTextColor(if (hasSms) okColor else errorColor)
        binding.btnGrantSms.visibility = if (hasSms) View.GONE else View.VISIBLE

        val hasNotif = hasNotificationListenerAccess(requireContext())
        binding.ivNotifStatus.setImageResource(
            if (hasNotif) R.drawable.ic_status_ok else R.drawable.ic_status_error
        )
        binding.tvNotifStatus.text = getString(
            if (hasNotif) R.string.notif_access_granted else R.string.notif_access_missing
        )
        binding.tvNotifStatus.setTextColor(if (hasNotif) okColor else errorColor)
        binding.btnGrantNotification.visibility = if (hasNotif) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
