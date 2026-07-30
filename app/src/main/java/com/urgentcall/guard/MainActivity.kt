package com.urgentcall.guard

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.media.AudioManager
import android.content.Context

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var serviceSwitch: Switch

    private val requiredPermissions = buildList {
        add(android.Manifest.permission.READ_PHONE_STATE)
        add(android.Manifest.permission.READ_CALL_LOG)
        add(android.Manifest.permission.SEND_SMS)
        add(android.Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        serviceSwitch = findViewById(R.id.serviceSwitch)
        val requestPermsButton = findViewById<android.widget.Button>(R.id.requestPermsButton)
        val requestDndButton = findViewById<android.widget.Button>(R.id.requestDndButton)
        val settingsButton = findViewById<android.widget.Button>(R.id.settingsButton)
        val whitelistButton = findViewById<android.widget.Button>(R.id.whitelistButton)

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        whitelistButton.setOnClickListener {
            startActivity(Intent(this, WhitelistActivity::class.java))
        }

        serviceSwitch.isChecked = PreferencesHelper.isServiceEnabled(this)
        serviceSwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            PreferencesHelper.setServiceEnabled(this, isChecked)
            if (isChecked) startGuardService() else stopService(Intent(this, UrgentCallForegroundService::class.java))
            refreshStatus()
        }

        requestPermsButton.setOnClickListener {
            permissionLauncher.launch(requiredPermissions)
        }

        requestDndButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }

        if (serviceSwitch.isChecked) startGuardService()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun startGuardService() {
        val intent = Intent(this, UrgentCallForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun refreshStatus() {
        val missing = requiredPermissions.filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val dndGranted = audioManager.isNotificationPolicyAccessGranted

        statusText.text = when {
            missing.isNotEmpty() -> getString(R.string.status_missing_permissions, missing.size)
            !dndGranted -> getString(R.string.status_missing_dnd)
            else -> getString(R.string.status_ready)
        }
    }
}
