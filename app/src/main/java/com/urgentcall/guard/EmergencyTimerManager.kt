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

    // Un token de Runnable par numéro, pour pouvoir annuler proprement le
    // restore programmé quand la fenêtre est fermée avant son expiration
    // (rappel réussi), au lieu de laisser le Runnable s'exécuter pour rien.
    private val pendingTokens = mutableMapOf<String, Any>()

    private fun normalize(number: String): String = number.filter { it.isDigit() }.takeLast(9)

    fun startRecallTimer(
        context: Context,
        phoneNumber: String,
        timerMinutes: Int,
        previousRingerMode: Int,
        previousVolumePercent: Int
    ) {
        // On capture toujours l'applicationContext : ce Runnable peut vivre
        // jusqu'à timerMinutes (potentiellement plusieurs centaines de minutes),
        // il ne doit jamais retenir un Context plus court-vécu que l'app elle-même.
        val appContext = context.applicationContext
        val normalized = normalize(phoneNumber)

        cancelPending(normalized)
        activeWindows.removeAll { it.phoneNumber == normalized }
        val expiresAt = System.currentTimeMillis() + timerMinutes * 60_000L
        activeWindows.add(ActiveWindow(normalized, expiresAt))

        val token = Any()
        pendingTokens[normalized] = token
        handler.postDelayed({
            if (pendingTokens[normalized] === token) {
                activeWindows.removeAll { it.phoneNumber == normalized }
                pendingTokens.remove(normalized)
                AudioManagerHelper.restoreInitialAudioSettings(appContext)
                UrgentCallForegroundService.refreshNotification(appContext)
            }
        }, timerMinutes * 60_000L)
    }

    fun isUrgentRecallWindowActive(phoneNumber: String): Boolean {
        val normalized = normalize(phoneNumber)
        val now = System.currentTimeMillis()
        activeWindows.removeAll { it.expiresAtMs < now }
        return activeWindows.any { it.phoneNumber == normalized }
    }

    /** Ferme la fenêtre immédiatement (rappel traité) et annule le restore différé en attente. */
    fun clearWindow(context: Context, phoneNumber: String) {
        val normalized = normalize(phoneNumber)
        cancelPending(normalized)
        activeWindows.removeAll { it.phoneNumber == normalized }
        UrgentCallForegroundService.refreshNotification(context.applicationContext)
    }

    private fun cancelPending(normalized: String) {
        // Invalide le token : le Runnable déjà en file dans le Handler se
        // reconnaîtra périmé à l'exécution et ne fera rien (pas de double restore).
        pendingTokens.remove(normalized)
    }
}
