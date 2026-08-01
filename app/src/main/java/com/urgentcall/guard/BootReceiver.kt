package com.urgentcall.guard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Relance le service permanent au démarrage du téléphone, si la surveillance est activée. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && PreferencesHelper.isServiceEnabled(context)) {
            UrgentCallForegroundService.start(context)
        }
    }
}
