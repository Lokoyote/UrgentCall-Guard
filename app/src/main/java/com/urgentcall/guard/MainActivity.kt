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
    private lateinit var permissionsCard: com.google.android.material.card.MaterialCardView
    private lateinit var permissionsListContainer: LinearLayout
    private lateinit var requestPermsButton: MaterialButton
    private lateinit var requestDndButton: MaterialButton
    private lateinit var requestBatteryButton: MaterialButton
    private lateinit var donationButton: MaterialButton
    private lateinit var howItWorksHeader: LinearLayout
    private lateinit var howItWorksBody: TextView
    private lateinit var howItWorksChevron: TextView
    private lateinit var resourcesHeader: LinearLayout
    private lateinit var resourcesBody: LinearLayout
    private lateinit var resourcesChevron: TextView

    // --- Ressources (autres bloqueurs d'appels) ---
    private lateinit var saracrocheWebsiteButton: MaterialButton
    private lateinit var saracrocheFdroidButton: MaterialButton
    private lateinit var callBlockerFdroidButton: MaterialButton
    private lateinit var callBlockerSourceButton: MaterialButton
    private lateinit var wincallsWebsiteButton: MaterialButton
    private lateinit var wincallsPlayStoreButton: MaterialButton
    private lateinit var fdroidExploreButton: MaterialButton

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshAll()
        // Si la notification initiale a été supprimée silencieusement par le
        // système faute de POST_NOTIFICATIONS au moment du démarrage du
        // service, on la reposte maintenant qu'elle vient d'être accordée.
        UrgentCallForegroundService.refreshNotification(this)
    }

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
        permissionsCard = findViewById(R.id.permissionsCard)
        permissionsListContainer = findViewById(R.id.permissionsListContainer)
        requestPermsButton = findViewById(R.id.requestPermsButton)
        requestDndButton = findViewById(R.id.requestDndButton)
        requestBatteryButton = findViewById(R.id.requestBatteryButton)
        donationButton = findViewById(R.id.donationButton)
        howItWorksHeader = findViewById(R.id.howItWorksHeader)
        howItWorksBody = findViewById(R.id.howItWorksBody)
        howItWorksChevron = findViewById(R.id.howItWorksChevron)
        resourcesHeader = findViewById(R.id.resourcesHeader)
        resourcesBody = findViewById(R.id.resourcesBody)
        resourcesChevron = findViewById(R.id.resourcesChevron)
        saracrocheWebsiteButton = findViewById(R.id.saracrocheWebsiteButton)
        saracrocheFdroidButton = findViewById(R.id.saracrocheFdroidButton)
        callBlockerFdroidButton = findViewById(R.id.callBlockerFdroidButton)
        callBlockerSourceButton = findViewById(R.id.callBlockerSourceButton)
        wincallsWebsiteButton = findViewById(R.id.wincallsWebsiteButton)
        wincallsPlayStoreButton = findViewById(R.id.wincallsPlayStoreButton)
        fdroidExploreButton = findViewById(R.id.fdroidExploreButton)

        applyHowItWorksState(PreferencesHelper.isHowItWorksExpanded(this))
        howItWorksHeader.setOnClickListener {
            val newState = howItWorksBody.visibility != View.VISIBLE
            PreferencesHelper.setHowItWorksExpanded(this, newState)
            applyHowItWorksState(newState)
        }

        applyResourcesState(PreferencesHelper.isResourcesExpanded(this))
        resourcesHeader.setOnClickListener {
            val newState = resourcesBody.visibility != View.VISIBLE
            PreferencesHelper.setResourcesExpanded(this, newState)
            applyResourcesState(newState)
        }

        val settingsCard = findViewById<LinearLayout>(R.id.settingsCard)
        val whitelistCard = findViewById<LinearLayout>(R.id.whitelistCard)
        val blacklistCard = findViewById<LinearLayout>(R.id.blacklistCard)

        serviceSwitch.isChecked = PreferencesHelper.isServiceEnabled(this)
        serviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesHelper.setServiceEnabled(this, isChecked)
            if (isChecked) {
                UrgentCallForegroundService.start(this)
            } else {
                UrgentCallForegroundService.stop(this)
            }
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
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.me/lokoyote")))
        }

        saracrocheWebsiteButton.setOnClickListener { openUrl(getString(R.string.resource_saracroche_website)) }
        saracrocheFdroidButton.setOnClickListener { openUrl(getString(R.string.resource_saracroche_fdroid)) }
        callBlockerFdroidButton.setOnClickListener { openUrl(getString(R.string.resource_callblocker_fdroid)) }
        callBlockerSourceButton.setOnClickListener { openUrl(getString(R.string.resource_callblocker_source)) }
        wincallsWebsiteButton.setOnClickListener { openUrl(getString(R.string.resource_wincalls_website)) }
        wincallsPlayStoreButton.setOnClickListener { openUrl(getString(R.string.resource_wincalls_playstore)) }
        fdroidExploreButton.setOnClickListener { openUrl(getString(R.string.resource_fdroid_explore_url)) }

        settingsCard.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        whitelistCard.setOnClickListener {
            startActivity(Intent(this, ListsActivity::class.java).putExtra(ListsActivity.EXTRA_INITIAL_TAB, ListsActivity.TAB_WHITELIST))
        }
        blacklistCard.setOnClickListener {
            startActivity(Intent(this, ListsActivity::class.java).putExtra(ListsActivity.EXTRA_INITIAL_TAB, ListsActivity.TAB_BLACKLIST))
        }

        if (serviceSwitch.isChecked) {
            UrgentCallForegroundService.start(this)
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

    private fun applyHowItWorksState(expanded: Boolean) {
        howItWorksBody.visibility = if (expanded) View.VISIBLE else View.GONE
        howItWorksChevron.text = getString(if (expanded) R.string.chevron_up else R.string.chevron_down)
    }

    private fun applyResourcesState(expanded: Boolean) {
        resourcesBody.visibility = if (expanded) View.VISIBLE else View.GONE
        resourcesChevron.text = getString(if (expanded) R.string.chevron_up else R.string.chevron_down)
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, url, android.widget.Toast.LENGTH_LONG).show()
        }
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
        if (PreferencesHelper.isServiceEnabled(this)) {
            UrgentCallForegroundService.refreshNotification(this)
        }
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

        permissionsCard.visibility = if (allGranted) View.GONE else View.VISIBLE

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
