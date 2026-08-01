package com.urgentcall.guard

import android.os.Bundle
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var volumeSeekBar: SeekBar
    private lateinit var volumeValueText: TextView
    private lateinit var timerInput: EditText
    private lateinit var smsTemplateInput: EditText
    private lateinit var immediateWhitelistSwitch: Switch
    private lateinit var systemFavoritesSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        title = getString(R.string.settings_title)

        volumeSeekBar = findViewById(R.id.volumeThresholdSeekBar)
        volumeValueText = findViewById(R.id.volumeThresholdValue)
        timerInput = findViewById(R.id.timerMinutesInput)
        smsTemplateInput = findViewById(R.id.smsTemplateInput)
        immediateWhitelistSwitch = findViewById(R.id.immediateWhitelistSwitch)
        systemFavoritesSwitch = findViewById(R.id.systemFavoritesSwitch)
        val saveButton = findViewById<android.widget.Button>(R.id.saveSettingsButton)

        // Chargement des valeurs actuelles
        val currentThreshold = PreferencesHelper.getVolumeThreshold(this)
        volumeSeekBar.progress = currentThreshold
        volumeValueText.text = "$currentThreshold%"
        timerInput.setText(PreferencesHelper.getTimerMinutes(this).toString())
        smsTemplateInput.setText(PreferencesHelper.getSmsTemplate(this))
        immediateWhitelistSwitch.isChecked = PreferencesHelper.isImmediateWhitelistBreakthrough(this)
        systemFavoritesSwitch.isChecked = PreferencesHelper.isAllowSystemFavorites(this)

        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                volumeValueText.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        saveButton.setOnClickListener { saveSettings() }
    }

    private fun saveSettings() {
        val threshold = volumeSeekBar.progress
        val timerMinutes = timerInput.text.toString().toIntOrNull()?.coerceIn(1, 999) ?: 5
        val smsTemplate = smsTemplateInput.text.toString().ifBlank {
            PreferencesHelper.getSmsTemplate(this)
        }
        val immediateWhitelist = immediateWhitelistSwitch.isChecked
        val systemFavorites = systemFavoritesSwitch.isChecked

        PreferencesHelper.setVolumeThreshold(this, threshold)
        PreferencesHelper.setTimerMinutes(this, timerMinutes)
        PreferencesHelper.setSmsTemplate(this, smsTemplate)
        PreferencesHelper.setImmediateWhitelistBreakthrough(this, immediateWhitelist)
        PreferencesHelper.setAllowSystemFavorites(this, systemFavorites)

        // Met à jour immédiatement la notification permanente (nouveau seuil pris en compte)
        UrgentCallForegroundService.refreshNotification(this)

        Toast.makeText(this, R.string.settings_saved_toast, Toast.LENGTH_SHORT).show()
        finish()
    }
}
