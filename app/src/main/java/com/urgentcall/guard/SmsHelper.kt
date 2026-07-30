package com.urgentcall.guard

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

/**
 * Envoie le SMS d'alerte d'urgence à l'appelant manqué.
 * Nécessite la permission SEND_SMS (demandée dans MainActivity).
 */
object SmsHelper {

    fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            val smsManager: SmsManager =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
        } catch (e: SecurityException) {
            Log.e("UrgentCallGuard", "Permission SEND_SMS manquante, SMS non envoyé", e)
        } catch (e: Exception) {
            Log.e("UrgentCallGuard", "Échec de l'envoi du SMS d'urgence", e)
        }
    }
}
