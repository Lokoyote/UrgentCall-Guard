package com.urgentcall.guard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Service PERMANENT : tourne en continu tant que la surveillance est activée,
 * discrètement (notification basse priorité, sans son), pour garantir que
 * l'app reste éveillée en arrière-plan en attendant un appel entrant.
 *
 * Volontairement, il n'utilise PAS d'alarme d'auto-relance : START_STICKY
 * suffit à demander au système de le relancer s'il est tué, sans le coût
 * batterie d'un réveil forcé périodique (voir l'historique de cette conv.
 * pour le diagnostic qui a mené à retirer ce mécanisme la première fois).
 */
class UrgentCallForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "urgent_call_guard_foreground"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, UrgentCallForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UrgentCallForegroundService::class.java))
        }

        /** Force une mise à jour immédiate du contenu (ex: après changement de seuil dans les réglages). */
        fun refreshNotification(context: Context) {
            if (!PreferencesHelper.isServiceEnabled(context)) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification(context))
        }

        private fun isLowVolumeOrSilent(context: Context): Boolean {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val ringerMode = audioManager.ringerMode
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
            val volumePercent = if (maxVolume > 0) (currentVolume * 100) / maxVolume else 0
            val threshold = PreferencesHelper.getVolumeThreshold(context)
            return ringerMode != AudioManager.RINGER_MODE_NORMAL || volumePercent <= threshold
        }

        private fun buildNotification(context: Context): Notification {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val lowVolume = isLowVolumeOrSilent(context)
            val title = context.getString(
                if (lowVolume) R.string.notif_title_low_volume else R.string.notif_title_monitoring
            )
            val text = context.getString(
                if (lowVolume) R.string.notif_text_low_volume else R.string.notif_text_monitoring
            )
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_shield)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(contentIntent)
                .build()
        }
    }

    // Se déclenche à chaque changement de volume ou de mode sonnerie pour
    // que la notification reflète toujours l'état réel, sans attendre un appel.
    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshNotification(context)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val filter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stateReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(this))
        // START_STICKY : le système relance ce service s'il le tue pour
        // libérer de la mémoire, sans réveil forcé ni alarme personnalisée.
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(stateReceiver) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
