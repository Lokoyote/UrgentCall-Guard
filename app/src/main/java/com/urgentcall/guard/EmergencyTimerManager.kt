package com.urgentcall.guard

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Gère la fenêtre temporaire pendant laquelle un rappel du même numéro
 * force le téléphone à sonner à volume max, même en mode silencieux.
 */
object EmergencyTimerManager {

    private data class ActiveWindow(val phoneNumber: String, val expiresAtMs: Long)

    private val activeWindows = mutableListOf<ActiveWindow>()
    private val handler = Handler(Looper.getMainLooper())

    private fun normalize(number: String): String = number.filter { it.isDigit() }.takeLast(9)

    fun startRecallTimer(
        context: Context,
        phoneNumber: String,
        timerMinutes: Int,
        previousRingerMode: Int,
        previousVolumePercent: Int
    ) {
        val normalized = normalize(phoneNumber)
        activeWindows.removeAll { it.phoneNumber == normalized }
        val expiresAt = System.currentTimeMillis() + timerMinutes * 60_000L
        activeWindows.add(ActiveWindow(normalized, expiresAt))

        handler.postDelayed({
            // Si personne n'a rappelé avant expiration, on nettoie et on restaure l'état initial.
            val stillActive = activeWindows.any { it.phoneNumber == normalized && it.expiresAtMs <= System.currentTimeMillis() + 500 }
            if (stillActive) {
                activeWindows.removeAll { it.phoneNumber == normalized }
                AudioManagerHelper.restoreInitialAudioSettings(context)
            }
        }, timerMinutes * 60_000L)
    }

    fun isUrgentRecallWindowActive(phoneNumber: String): Boolean {
        val normalized = normalize(phoneNumber)
        val now = System.currentTimeMillis()
        activeWindows.removeAll { it.expiresAtMs < now }
        return activeWindows.any { it.phoneNumber == normalized }
    }

    fun clearWindow(phoneNumber: String) {
        val normalized = normalize(phoneNumber)
        activeWindows.removeAll { it.phoneNumber == normalized }
    }
}
