package com.urgentcall.guard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var currentVolumeValue: TextView
    private lateinit var thresholdValue: TextView
    private lateinit var timerValue: TextView
    private lateinit var serviceSwitch: SwitchMaterial
    private lateinit var allGrantedBanner: TextView
    private lateinit var permissionsListContainer: LinearLayout
    private lateinit var requestPermsButton: MaterialButton
    private lateinit var requestDndButton: MaterialButton
    private lateinit var requestBatteryButton: MaterialButton
    private lateinit var donationButton: MaterialButton

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshAll() }

    // Se déclenche dès que le volume système change (boutons physiques, autre app, etc.)
    // pour actualiser l'affichage du volume actuel en temps réel, sans attendre onResume().
    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
                refreshSummary()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentVolumeValue = findViewById(R.id.currentVolumeValue)
        thresholdValue = findViewById(R.id.thresholdValue)
        timerValue = findViewById(R.id.timerValue)
        serviceSwitch = findViewById(R.id.serviceSwitch)
        allGrantedBanner = findViewById(R.id.allGrantedBanner)
        permissionsListContainer = findViewById(R.id.permissionsListContainer)
        requestPermsButton = findViewById(R.id.requestPermsButton)
        requestDndButton = findViewById(R.id.requestDndButton)
        requestBatteryButton = findViewById(R.id.requestBatteryButton)
        donationButton = findViewById(R.id.donationButton)

        val settingsCard = findViewById<LinearLayout>(R.id.settingsCard)
        val whitelistCard = findViewById<LinearLayout>(R.id.whitelistCard)
        val blacklistCard = findViewById<LinearLayout>(R.id.blacklistCard)

        serviceSwitch.isChecked = PreferencesHelper.isServiceEnabled(this)
        serviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Rien d'autre à faire ici : CallBroadcastReceiver relit ce réglage
            // à chaque appel entrant. Aucun service permanent à démarrer/arrêter.
            PreferencesHelper.setServiceEnabled(this, isChecked)
        }

        requestPermsButton.setOnClickListener {
            permissionLauncher.launch(PermissionStatusHelper.runtimePermissions())
        }

        requestDndButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }

        requestBatteryButton.setOnClickListener {
            requestIgnoreBatteryOptimizations()
        }

        donationButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.me/AdrienCUFFARO")))
        }

        settingsCard.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        whitelistCard.setOnClickListener {
            startActivity(Intent(this, ListsActivity::class.java).putExtra(ListsActivity.EXTRA_INITIAL_TAB, ListsActivity.TAB_WHITELIST))
        }
        blacklistCard.setOnClickListener {
            startActivity(Intent(this, ListsActivity::class.java).putExtra(ListsActivity.EXTRA_INITIAL_TAB, ListsActivity.TAB_BLACKLIST))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(volumeReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(volumeReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(volumeReceiver)
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    private fun refreshAll() {
        refreshSummary()
        refreshPermissions()
    }

    private fun refreshSummary() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        val currVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val volumePercent = if (maxVol > 0) (currVol * 100) / maxVol else 0

        currentVolumeValue.text = "$volumePercent%"
        thresholdValue.text = "${PreferencesHelper.getVolumeThreshold(this)}%"
        timerValue.text = "${PreferencesHelper.getTimerMinutes(this)}"
    }

    private fun refreshPermissions() {
        val statuses = PermissionStatusHelper.getAllStatuses(this)
        val allGranted = statuses.all { it.granted }

        allGrantedBanner.visibility = if (allGranted) View.VISIBLE else View.GONE
        permissionsListContainer.visibility = if (allGranted) View.GONE else View.VISIBLE

        permissionsListContainer.removeAllViews()
        if (!allGranted) {
            val inflater = LayoutInflater.from(this)
            statuses.forEach { status ->
                val row = inflater.inflate(R.layout.item_permission_status, permissionsListContainer, false)
                row.findViewById<TextView>(R.id.permissionIcon).text = if (status.granted) "✅" else "⚠️"
                row.findViewById<TextView>(R.id.permissionLabel).text = status.label
                val stateText = row.findViewById<TextView>(R.id.permissionState)
                if (status.granted) {
                    stateText.text = getString(R.string.permission_granted_state)
                    stateText.setTextColor(getColor(R.color.emerald_600))
                } else {
                    stateText.text = getString(R.string.permission_missing_state)
                    stateText.setTextColor(getColor(R.color.amber_600))
                }
                permissionsListContainer.addView(row)
            }
        }

        // On n'affiche que les boutons pertinents pour ce qui manque encore.
        requestPermsButton.visibility =
            if (PermissionStatusHelper.hasMissingRuntimePermissions(this)) View.VISIBLE else View.GONE
        requestDndButton.visibility =
            if (!PermissionStatusHelper.isDndAccessGranted(this)) View.VISIBLE else View.GONE
        requestBatteryButton.visibility =
            if (!PermissionStatusHelper.isBatteryOptimizationIgnored(this)) View.VISIBLE else View.GONE
    }
}
