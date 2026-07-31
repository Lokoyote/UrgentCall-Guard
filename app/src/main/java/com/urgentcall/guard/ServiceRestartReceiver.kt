package com.urgentcall.guard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Déclenché par une alarme programmée depuis UrgentCallForegroundService.onDestroy().
 * Relance le service si la surveillance est censée rester active — utile quand
 * l'utilisateur glisse la notification pour la retirer (possible depuis Android 14)
 * ou que le système tue le service pour libérer de la mémoire.
 */
class ServiceRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!PreferencesHelper.isServiceEnabled(context)) return

        val serviceIntent = Intent(context, UrgentCallForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
