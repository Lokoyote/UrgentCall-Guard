package com.urgentcall.guard

import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class SettingsActivity : AppCompatActivity() {

    private lateinit var volumeSeekBar: SeekBar
    private lateinit var volumeValueText: TextView
    private lateinit var timerInput: EditText
    private lateinit var smsTemplateInput: EditText
    private lateinit var countrySearchInput: AutoCompleteTextView
    private lateinit var selectedCountriesChipGroup: ChipGroup

    // Codes ISO 3166-1 alpha-2 actuellement sélectionnés pour la détection mobile/fixe.
    private val selectedCountryCodes = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        title = getString(R.string.settings_title)

        volumeSeekBar = findViewById(R.id.volumeThresholdSeekBar)
        volumeValueText = findViewById(R.id.volumeThresholdValue)
        timerInput = findViewById(R.id.timerMinutesInput)
        smsTemplateInput = findViewById(R.id.smsTemplateInput)
        countrySearchInput = findViewById(R.id.countrySearchInput)
        selectedCountriesChipGroup = findViewById(R.id.selectedCountriesChipGroup)
        val saveButton = findViewById<android.widget.Button>(R.id.saveSettingsButton)

        // Chargement des valeurs actuelles
        val currentThreshold = PreferencesHelper.getVolumeThreshold(this)
        volumeSeekBar.progress = currentThreshold
        volumeValueText.text = "$currentThreshold%"
        timerInput.setText(PreferencesHelper.getTimerMinutes(this).toString())
        smsTemplateInput.setText(PreferencesHelper.getSmsTemplate(this))

        selectedCountryCodes.addAll(
            PreferencesHelper.getMobileDetectionCountries(this).ifEmpty { setOf("FR") }
        )
        refreshChips()

        CountrySearchHelper.setupSearchInput(this, countrySearchInput, { selectedCountryCodes }) { code ->
            addCountry(code)
        }

        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                volumeValueText.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        saveButton.setOnClickListener { saveSettings() }
    }

    private fun addCountry(code: String) {
        if (selectedCountryCodes.add(code)) {
            refreshChips()
        }
    }

    private fun refreshChips() {
        selectedCountriesChipGroup.removeAllViews()
        selectedCountryCodes
            .sortedBy { CountrySearchHelper.label(this, it) }
            .forEach { code ->
                val chip = Chip(this).apply {
                    text = CountrySearchHelper.label(this@SettingsActivity, code)
                    isCloseIconVisible = true
                    setOnCloseIconClickListener {
                        selectedCountryCodes.remove(code)
                        refreshChips()
                    }
                }
                selectedCountriesChipGroup.addView(chip)
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
        PreferencesHelper.setMobileDetectionCountries(this, selectedCountryCodes)

        // Met à jour immédiatement la notification permanente (nouveau seuil pris en compte)
        UrgentCallForegroundService.refreshNotification(this)

        Toast.makeText(this, R.string.settings_saved_toast, Toast.LENGTH_SHORT).show()
        finish()
    }
}
