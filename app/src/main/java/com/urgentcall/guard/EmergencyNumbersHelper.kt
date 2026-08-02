package com.urgentcall.guard

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager

/**
 * Détecte les numéros d'urgence (112, 15, 17, 18, 911, 999...) pour qu'ils
 * franchissent TOUJOURS le silencieux, comme s'ils étaient en liste blanche,
 * sans que l'utilisateur ait besoin de les ajouter manuellement.
 */
object EmergencyNumbersHelper {

    // Repli pour les appareils trop anciens pour TelephonyManager.isEmergencyNumber
    // (API 29+) : numéros d'urgence les plus courants dans le monde.
    private val FALLBACK_EMERGENCY_NUMBERS = setOf(
        "112", "911", "999", "000", "110", "118", "119",
        "17", "18", "15", "122", "133", "197"
    )

    fun isEmergencyNumber(context: Context, phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                telephonyManager.isEmergencyNumber(phoneNumber)
            } else {
                phoneNumber.filter { it.isDigit() } in FALLBACK_EMERGENCY_NUMBERS
            }
        } catch (e: Exception) {
            phoneNumber.filter { it.isDigit() } in FALLBACK_EMERGENCY_NUMBERS
        }
    }
}
