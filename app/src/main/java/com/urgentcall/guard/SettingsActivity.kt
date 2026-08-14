package com.urgentcall.guard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var volumeSeekBar: SeekBar
    private lateinit var volumeValueText: TextView
    private lateinit var timerInput: EditText
    private lateinit var smsTemplateInput: EditText

    // Association case à cocher <-> code pays ISO 3166-1 alpha-2, pour la détection mobile/fixe.
    private lateinit var countryCheckboxes: List<Pair<CheckBox, String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        title = getString(R.string.settings_title)

        volumeSeekBar = findViewById(R.id.volumeThresholdSeekBar)
        volumeValueText = findViewById(R.id.volumeThresholdValue)
        timerInput = findViewById(R.id.timerMinutesInput)
        smsTemplateInput = findViewById(R.id.smsTemplateInput)
        val saveButton = findViewById<android.widget.Button>(R.id.saveSettingsButton)
        val openDndSettingsButton = findViewById<android.widget.Button>(R.id.openDndSettingsButton)

        // Chargement des valeurs actuelles
        val currentThreshold = PreferencesHelper.getVolumeThreshold(this)
        volumeSeekBar.progress = currentThreshold
        volumeValueText.text = "$currentThreshold%"
        timerInput.setText(PreferencesHelper.getTimerMinutes(this).toString())
        smsTemplateInput.setText(PreferencesHelper.getSmsTemplate(this))

        countryCheckboxes = listOf(
            findViewById<CheckBox>(R.id.countryCheckboxFR) to "FR",
            findViewById<CheckBox>(R.id.countryCheckboxBE) to "BE",
            findViewById<CheckBox>(R.id.countryCheckboxCH) to "CH",
            findViewById<CheckBox>(R.id.countryCheckboxLU) to "LU",
            findViewById<CheckBox>(R.id.countryCheckboxCA) to "CA",
            findViewById<CheckBox>(R.id.countryCheckboxMA) to "MA",
            findViewById<CheckBox>(R.id.countryCheckboxDZ) to "DZ",
            findViewById<CheckBox>(R.id.countryCheckboxTN) to "TN"
        )
        val selectedCountries = PreferencesHelper.getMobileDetectionCountries(this)
        countryCheckboxes.forEach { (checkbox, code) -> checkbox.isChecked = code in selectedCountries }

        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                volumeValueText.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        saveButton.setOnClickListener { saveSettings() }

        openDndSettingsButton.setOnClickListener {
            // Ouvre directement l'écran système "Ne pas déranger" (pour l'activer/le
            // configurer) — différent de l'écran d'autorisation ACCESS_NOTIFICATION_POLICY,
            // déjà géré par un bouton dédié sur l'écran d'accueil.
            startActivity(Intent(Settings.ACTION_ZEN_MODE_SETTINGS))
        }
    }

    private fun saveSettings() {
        val threshold = volumeSeekBar.progress
        val timerMinutes = timerInput.text.toString().toIntOrNull()?.coerceIn(1, 999) ?: 5
        val smsTemplate = smsTemplateInput.text.toString().ifBlank {
            PreferencesHelper.getSmsTemplate(this)
        }

        PreferencesHelper.setVolumeThreshold(this, threshold)
        PreferencesHelper.setTimerMinutes(this, timerMinutes)
        PreferencesHelper.setSmsTemplate(this, smsTemplate)

        val selectedCountries = countryCheckboxes.filter { (checkbox, _) -> checkbox.isChecked }
            .map { (_, code) -> code }
            .toSet()
        PreferencesHelper.setMobileDetectionCountries(this, selectedCountries)

        // Met à jour immédiatement la notification permanente (nouveau seuil pris en compte)
        UrgentCallForegroundService.refreshNotification(this)

        Toast.makeText(this, R.string.settings_saved_toast, Toast.LENGTH_SHORT).show()
        finish()
    }
}
