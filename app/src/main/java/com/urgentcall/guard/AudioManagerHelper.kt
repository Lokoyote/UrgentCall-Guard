package com.urgentcall.guard

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

object AudioManagerHelper {

    private var savedRingerMode: Int = AudioManager.RINGER_MODE_SILENT
    private var savedVolumePercent: Int = 0
    private var isAudioOverridden: Boolean = false

    private fun hasDndAccess(context: Context): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun forceMaxVolumeRingtone(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Nécessite l'accès à la politique de notifications (DND) sur Android 6+
        if (!hasDndAccess(context)) return

        if (!isAudioOverridden) {
            savedRingerMode = audioManager.ringerMode
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            val currVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
            savedVolumePercent = if (maxVol > 0) (currVol * 100) / maxVol else 0
            isAudioOverridden = true
        }

        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume, AudioManager.FLAG_SHOW_UI)
    }

    fun restoreInitialAudioSettings(context: Context) {
        if (!isAudioOverridden) return

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (!hasDndAccess(context)) {
            isAudioOverridden = false
            return
        }
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)

        audioManager.ringerMode = savedRingerMode
        if (savedRingerMode == AudioManager.RINGER_MODE_NORMAL) {
            val restoredVol = (savedVolumePercent * maxVol) / 100
            audioManager.setStreamVolume(AudioManager.STREAM_RING, restoredVol, 0)
        }

        isAudioOverridden = false
    }

    fun scheduleAudioSettingsRestore(context: Context, delaySec: Int = 3) {
        Handler(Looper.getMainLooper()).postDelayed({
            restoreInitialAudioSettings(context)
        }, delaySec * 1000L)
    }
}
