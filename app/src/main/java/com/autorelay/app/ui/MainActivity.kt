package com.autorelay.app.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.autorelay.app.R
import com.autorelay.app.data.LogEntry
import com.autorelay.app.data.RelayConfig
import com.autorelay.app.data.RelayLog
import com.autorelay.app.databinding.ActivityMainBinding
import com.autorelay.app.engine.RelayEngine
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AutoRelay"
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)

        binding.viewPager.adapter = MainPagerAdapter(this)

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_config)
                1 -> getString(R.string.tab_log)
                else -> ""
            }
        }.attach()

        supportFragmentManager.addOnBackStackChangedListener {
            binding.containerPermissions.visibility =
                if (supportFragmentManager.backStackEntryCount > 0) View.VISIBLE else View.GONE
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val config = RelayConfig(this)
        menu.findItem(R.id.action_hide_unknown_sender)?.isChecked = config.hideUnknownSender
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_test_relay -> {
                RelayEngine.processIncomingMessage(
                    this, "Test Sender",
                    getString(R.string.test_relay_body),
                    LogEntry.Source.SMS
                )
                Snackbar.make(binding.main, R.string.test_relay_initiated, Snackbar.LENGTH_SHORT).show()
                true
            }
            R.id.action_clear_log -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.menu_clear_log)
                    .setMessage(R.string.dialog_clear_log_message)
                    .setPositiveButton(R.string.clear) { _, _ -> RelayLog.clear() }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                true
            }
            R.id.action_hide_unknown_sender -> {
                val config = RelayConfig(this)
                config.hideUnknownSender = !item.isChecked
                item.isChecked = config.hideUnknownSender
                RelayLog.refresh()
                true
            }
            R.id.action_about -> {
                openAbout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun openPermissions() {
        binding.containerPermissions.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(R.id.containerPermissions, PermissionsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun openAbout() {
        binding.containerPermissions.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(R.id.containerPermissions, AboutFragment())
            .addToBackStack(null)
            .commit()
    }
}
