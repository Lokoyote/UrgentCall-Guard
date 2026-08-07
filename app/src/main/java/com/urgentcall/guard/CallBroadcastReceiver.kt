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

                if (number != null && EmergencyNumbersHelper.isEmergencyNumber(context, number)) {
                    // Numéro d'urgence : franchissement systématique, même si (par erreur)
                    // le numéro était en liste noire. Priorité absolue, aucune exception.
                    AudioManagerHelper.forceMaxVolumeRingtone(context)
                    lastState = state
                    return
                }

                val isBlocked = number != null && BlacklistHelper.isBlacklisted(context, number)

                if (!isBlocked && number != null && EmergencyTimerManager.isUrgentRecallWindowActive(number)) {
                    // Rappel dans la fenêtre d'urgence -> FORCE le volume max
                    AudioManagerHelper.forceMaxVolumeRingtone(context)
                    // Le rappel attendu vient d'arriver : on ferme la fenêtre tout de
                    // suite (annule le restore différé encore en attente et arrête
                    // le service éphémère si plus aucune fenêtre n'est active).
                    EmergencyTimerManager.clearWindow(context, number)
                } else if (!isBlocked && number != null && WhitelistHelper.isWhitelisted(context, number)) {
                    // Contact prioritaire (liste blanche manuelle ou favori synchronisé) -> franchissement immédiat
                    AudioManagerHelper.forceMaxVolumeRingtone(context)
                } else if (!isBlocked && number != null && PreferencesHelper.isAllowSystemFavorites(context) &&
                    ContactsHelper.isSystemFavorite(context, number)
                ) {
                    // Filet de sécurité : les favoris sont normalement synchronisés dans la
                    // liste blanche (WhitelistHelper.syncSystemFavorites), donc déjà couverts
                    // par la branche ci-dessus. Ce cas ne sert qu'en cas de synchro pas encore faite.
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
                    // Appel manqué détecté (number peut être null si le numéro est masqué)
                    handleMissedCall(context, number)
                } else if (lastState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    AudioManagerHelper.restoreInitialAudioSettings(context)
                }
            }
        }
        lastState = state
    }

    private fun handleMissedCall(context: Context, phoneNumber: String?) {
        // 0) Numéro d'urgence : jamais de SMS automatique (déjà géré en priorité à la sonnerie)
        if (phoneNumber != null && EmergencyNumbersHelper.isEmergencyNumber(context, phoneNumber)) {
            Log.i("UrgentCallGuard", "Numéro $phoneNumber d'urgence : pas de SMS automatique.")
            return
        }

        // 1) Numéro masqué / privé (pas de numéro exploitable)
        if (BlacklistHelper.isHiddenNumber(phoneNumber)) {
            if (BlacklistHelper.isBlockHiddenNumbers(context)) {
                Log.i("UrgentCallGuard", "Numéro masqué : SMS/volume ignorés (réglage liste noire).")
                return
            }
        }
        val number = phoneNumber ?: return // impossible d'envoyer un SMS sans numéro, dans tous les cas

        // 2) Numéro explicitement dans la liste noire
        if (BlacklistHelper.isBlacklisted(context, number)) {
            Log.i("UrgentCallGuard", "Numéro $number en liste noire : SMS/volume ignorés.")
            return
        }

        // 3) Numéro inconnu (absent du répertoire), si l'option est activée
        if (BlacklistHelper.isBlockUnknownNumbers(context) && !ContactsHelper.isNumberInContacts(context, number)) {
            Log.i("UrgentCallGuard", "Numéro $number inconnu du répertoire : SMS/volume ignorés (réglage liste noire).")
            return
        }

        // 4) Garde explicite : si on est déjà dans la fenêtre de rappel pour ce numéro,
        // ce nouvel appel manqué EST le rappel attendu -> pas de second SMS.
        if (EmergencyTimerManager.isUrgentRecallWindowActive(number)) {
            Log.i("UrgentCallGuard", "Rappel de $number déjà en fenêtre d'urgence : pas de second SMS.")
            return
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentRingerMode = audioManager.ringerMode
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        val volumePercent = if (maxVolume > 0) (currentVolume * 100) / maxVolume else 0

        val threshold = PreferencesHelper.getVolumeThreshold(context)

        if (currentRingerMode != AudioManager.RINGER_MODE_NORMAL || volumePercent <= threshold) {
            val timerMins = PreferencesHelper.getTimerMinutes(context)
            val smsText = PreferencesHelper.getSmsTemplate(context).replace("{TIMER}", timerMins.toString())

            SmsHelper.sendSms(context, number, smsText)
            EmergencyTimerManager.startRecallTimer(context, number, timerMins, currentRingerMode, volumePercent)

            Log.i("UrgentCallGuard", "SMS d'urgence envoyé à $number. Fenêtre de $timerMins min activée.")
        } else {
            Log.i("UrgentCallGuard", "Volume sonore suffisant ($volumePercent% > $threshold%). Pas d'action.")
        }
    }
}
