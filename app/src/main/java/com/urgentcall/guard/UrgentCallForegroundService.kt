package com.urgentcall.guard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Service PONCTUEL, actif uniquement pendant une fenêtre de rappel d'urgence
 * (entre un appel manqué et sa fin de fenêtre / son rappel). Il n'est jamais
 * lancé au démarrage du téléphone ni maintenu en permanence : il est démarré
 * par EmergencyTimerManager.startRecallTimer() et stoppé par lui dès que la
 * fenêtre se ferme, pour garantir que l'app n'est active que le temps
 * nécessaire autour d'un véritable appel entrant.
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
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        // Pas de START_STICKY : ce service ne doit PAS être relancé automatiquement
        // par le système. Sa durée de vie est entièrement pilotée par
        // EmergencyTimerManager (fenêtre de rappel ouverte/fermée).
        return START_NOT_STICKY
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text_format))
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .build()
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

    override fun onBind(intent: Intent?): IBinder? = null
}
