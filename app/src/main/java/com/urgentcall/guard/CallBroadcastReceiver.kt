package com.urgentcall.guard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.telephony.TelephonyManager
import android.util.Log

class CallBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var incomingNumber: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!PreferencesHelper.isServiceEnabled(context)) return
        if (intent.action != "android.intent.action.PHONE_STATE") return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: incomingNumber

        val state = when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
            TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
            TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
            else -> TelephonyManager.CALL_STATE_IDLE
        }

        onCallStateChanged(context, state, number)
    }

    private fun onCallStateChanged(context: Context, state: Int, number: String?) {
        if (lastState == state) return

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                incomingNumber = number

                if (number != null && EmergencyTimerManager.isUrgentRecallWindowActive(number)) {
                    // Rappel dans la fenêtre d'urgence -> FORCE le volume max
                    AudioManagerHelper.forceMaxVolumeRingtone(context)
                } else if (number != null && WhitelistHelper.isWhitelistedAndImmediate(context, number)) {
                    // Contact prioritaire -> franchissement immédiat
                    AudioManagerHelper.forceMaxVolumeRingtone(context)
                }
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    val delay = PreferencesHelper.getAutoRestoreDelaySec(context)
                    AudioManagerHelper.scheduleAudioSettingsRestore(context, delay)
                }
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    // Appel manqué détecté
                    if (number != null) {
                        handleMissedCall(context, number)
                    }
                } else if (lastState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    AudioManagerHelper.restoreInitialAudioSettings(context)
                }
            }
        }
        lastState = state
    }

    private fun handleMissedCall(context: Context, phoneNumber: String) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentRingerMode = audioManager.ringerMode
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        val volumePercent = if (maxVolume > 0) (currentVolume * 100) / maxVolume else 0

        val threshold = PreferencesHelper.getVolumeThreshold(context)

        if (currentRingerMode != AudioManager.RINGER_MODE_NORMAL || volumePercent <= threshold) {
            val timerMins = PreferencesHelper.getTimerMinutes(context)
            val smsText = PreferencesHelper.getSmsTemplate(context).replace("{TIMER}", timerMins.toString())

            SmsHelper.sendSms(context, phoneNumber, smsText)
            EmergencyTimerManager.startRecallTimer(context, phoneNumber, timerMins, currentRingerMode, volumePercent)

            Log.i("UrgentCallGuard", "SMS d'urgence envoyé à $phoneNumber. Fenêtre de $timerMins min activée.")
        } else {
            Log.i("UrgentCallGuard", "Volume sonore suffisant ($volumePercent% > $threshold%). Pas d'action.")
        }
    }
}
